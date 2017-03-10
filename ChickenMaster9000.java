package scripts;

import com.sun.deploy.uitoolkit.impl.awt.AWTAppletAdapter;
import org.tribot.api.Timing;
import org.tribot.api2007.*;
import org.tribot.api2007.types.*;
import org.tribot.script.Script;
import org.tribot.script.ScriptManifest;
import org.tribot.script.interfaces.Painting;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Random;

/**
 * Created by tim on 2/22/2017.
 */
@ScriptManifest(category = "Combat", name = "Chicken Master 9000", authors = "Master 9000", version = 0.03,
        description = "Kills chickens in lumbridge - more locations soon | " +
                " Loots Feathers (Optional) | Round Robin (Optional) - Changes attack style periodically ")
public class ChickenMaster9000 extends Script implements Painting{
    final double VERSION = 0.03;
    int state;
    int[] CHICKENS = {2693, 2692};
    int[] FEATHER = {314};
    int AANIMATION = 386;
    RSTile CHICKEN_LOC;


    long startTime;
    Color bgColor = new Color(204, 187, 154);
    int looted;

    int[] startLvls = new int[4];
    int[] startXP = new int[4];

    // gui settings
    boolean lootFeathers,roundRobin, pureMode;
    boolean started = false;
    //roundRobin
    long lastSwitchMillis;
    long timeToNext;
    long nextSwitchMillis;

    boolean killed = false;
    boolean animated = false;

    Random r = new Random();

    @Override
    public void run() {
        init();

        do {
            state = getState();

            switch (state) {
                case -1: // should not happen, exit
                    break;
                case 0: // sleep
                    sleep(2500, 3000);
                    break;
                case 1: // walk to chickens
                    walkToChickens();
                    break;
                case 2: // loot
                    loot();
                    break;
                case 3: // fight
                    fight();
                    break;
                case 10: // combat idle, when we are in combat
                    sleepTillDeath();
                    break;
                case 11: // movement idle, caused when our char is moving
                    sleepTillStill();
                    break;
                case 12: // General idle, when we are animating
                    if (Player.getAnimation() == AANIMATION) animated = true;
                    sleep(500, 1000);
                    break;
                case 15:
                    setStyle();
                    break;
            }
        } while (state != -1);
    }

    String sState() {
        switch (state) {
            case -1:
                return "shutting down";
            case 0:
                return "Not logged in";
            case 1:
                return "Walking to chickens";
            case 2:
                return "Looting";
            case 3:
                return "Fighting";
            case 10:
                return "Combat Idle";
            case 11:
                return "Movement Idle";
            case 12:
                return "General Idle";
            case 15:
                return "Round Robin Switch";
        }
        return "";
    }

    int getState() {
        RSPlayer p = Player.getRSPlayer();
        if (Login.getLoginState() != Login.STATE.INGAME) return 0; // not logged in
        if ((p.isInCombat() || animated) && !killed) return 10; // in combat
        if (p.isMoving()) return 11; // moving
        if (p.getAnimation() != -1) return 12; // have an animation
        if (p.getPosition().distanceTo(CHICKEN_LOC) > 7) return 1; // too far from chickens
        if (lootFeathers && getFeather() != null) return 2;
        if (roundRobin && needSwitch()) return 15;
        return 3;
    }

    void init() {
        println("Thank you for choosing "+this.getScriptName());
        ChickenGUI gui = new ChickenGUI();
        gui.setup();
        while (!gui.started) {
            sleep(1000);
        }
        started = true;
        lootFeathers = gui.featherBox.isSelected();
        roundRobin = gui.roundRobin.isSelected();
        pureMode = gui.pureBox.isSelected();
        AANIMATION = Integer.parseInt(gui.animationField.getText());
        if (roundRobin) {
            setStyle();
        }
        println("looting: " + lootFeathers+" round robin: "+roundRobin + " pure mode: " + pureMode + " Anim: "+ AANIMATION);
        startTime = System.currentTimeMillis();
        looted = 0;
        startLvls = getLvls();
        startXP = getXp();
        CHICKEN_LOC = new RSTile(3228, 3298, 0);
    }

    void walkToChickens() {
        WebWalking.walkTo(CHICKEN_LOC);
        sleep(250, 500);
    }

    void loot() {
        RSGroundItem feather = getFeather();
        if (feather != null) {
            if (!feather.isClickable()) {
                Random r = new Random();
                if (r.nextInt(2) == 0) {
                    Camera.turnToTile(feather);
                    sleep(100);
                } else {
                    Walking.walkTo(feather);
                    sleepTillStill();
                }
            }
            feather.click("Take Feather");
            sleepTillLoot();
        }
    }

    void fight() {
        RSNPC chicken = getChicken();
        if (chicken != null) {
            if (!chicken.isClickable()) {
                Random r = new Random();
                if (r.nextInt(2) == 0) {
                    Camera.turnToTile(chicken);
                    sleep(100);
                } else {
                    Walking.walkTo(chicken);
                    sleepTillStill();
                }
            }
            chicken.click("Attack Chicken");
            killed = false;
            animated = false;
            wtf = 0;
            sleep(200);
        } else {
           // println("cannot find a chicken");
            sleep(2000);
        }
    }

    RSGroundItem getFeather() {
        RSGroundItem[] feathers = GroundItems.findNearest("Feather");
        RSTile me = Player.getPosition();
        for (RSGroundItem f : feathers) {
            if (f != null) {
                if (me.distanceTo(f) < 3) return f;
            }
        }
        return null;
    }

    RSNPC getChicken() {
        RSNPC[] chickens = NPCs.findNearest(CHICKENS);
        RSTile me = Player.getPosition();
        for(RSNPC c : chickens) {
            if (c != null) {
                RSTile pos = c.getPosition();
                if (!c.isInCombat() && me.distanceTo(c) < 8) {
                    if (inBounds(pos)) return c;
                }
            }
        }
        return null;
    }

    RSNPC getNextChicken(RSNPC current) {
        RSNPC[] chickens = NPCs.findNearest(CHICKENS);
        for (RSNPC c : chickens) {
            if (c != null && c != current && !c.isInCombat() && Player.getPosition().distanceTo(c) < 8) return c;
        }
        return null;
    }

    boolean inBounds(RSTile pos) {
        if (pos.getY() < 3295 && pos.getX() < 3231) return false;
        if (pos.getX() > 3236 || pos.getY() > 3301) return false;
        return true;
    }

    void sleepTillLoot() {
        int count = Inventory.getCount("Feather");
        for (int i = 0; i < 10; i++) {
            if (Player.isMoving() && i > 0) i--;
            int change = Inventory.getCount("Feather") - count;
            if (change > 0) {
                looted += change;
                return;
            }
            sleep(100);
        }
    }
    int wtf = 0;
    void sleepTillDeath() { // sleeps until our target is dead
        RSCharacter target = Combat.getTargetEntity();

        if (target == null) {
            sleep(200,400);
            println("target is null");
            wtf++;
            if (wtf > 8) {
                killed = true;
                animated = false;
            }
            return;
        }

        println("target is real");
        int factor = r.nextInt(10);
        RSPlayer pl = Player.getRSPlayer();
        for (int i = 0; i < 20; i++) { // our iterative sleep
            pl = Player.getRSPlayer();
            RSNPC next = getNextChicken((RSNPC) target);
            /*
            if it makes it out of this loop, that means we have not been
            in combat or animated for 1000-1250 ms
             */
            if (factor >= 7 && next != null && getFeather() == null) {
                if (!next.isOnScreen()) Camera.turnToTile(next);
                next.hover();
            }

            if ((pl.isInCombat() || pl.getAnimation() == AANIMATION) && i > 0) i = 0;
            println("Combat timer: "+i);
            if (target.getHealthPercent() == 0.0) {
                println("target is dead");
                killed = true;
                animated = false;
                if (factor < 7) sleep(1500,3000); // random chance to wait for loot
                return;
            }

            sleep(100,125);
        }
        animated = false;
        killed = true;
    }

    void sleepTillStill() {
        for (int i = 0; i < 15; i++) {
            if (!Player.getRSPlayer().isMoving())
                return;
            if (Player.getAnimation() == AANIMATION) animated = true;
            sleep(100);
        }
    }

    void setStyle() {
        Random r = new Random();
        int min = r.nextInt(60);
        int switchTo = r.nextInt(3);
        lastSwitchMillis = System.currentTimeMillis();
        timeToNext = (long) min * 60 * 1000;
        nextSwitchMillis = lastSwitchMillis + timeToNext;
        if (switchTo == 0) { // attack
            println("[ROUND ROBIN] Switched to attack training, next switch in: "+Timing.msToString(timeToNext));
            Combat.selectIndex(0);
            sleep(500,1000);
        } else if (switchTo == 1) { // strength
            println("[ROUND ROBIN] Switched to strength training, next switch in: "+Timing.msToString(timeToNext));
            Combat.selectIndex(1);
            sleep(500, 1000);
        } else { // defence
            if (pureMode) {
                println("[ROUND ROBIN] Not swapping anything, next switch in: "+Timing.msToString(timeToNext));
                return;
            }
            println("[ROUND ROBIN] Switched to defence training, next switch in: "+Timing.msToString(timeToNext));
            Combat.selectIndex(3);
            sleep(500, 1000);
        }
        Inventory.open();
    }

    boolean needSwitch() {
        if (!roundRobin) return false;
        if (System.currentTimeMillis() > nextSwitchMillis) return true;
        return false;
    }

    @Override
    public void onPaint(Graphics graphics) {
        if (started) {
            long currentTime = System.currentTimeMillis();
            long timeRan = currentTime - startTime;
            DecimalFormat df = new DecimalFormat("##.####");
            DecimalFormat df2 = new DecimalFormat("####.##");
            df.setRoundingMode(RoundingMode.DOWN);
            double hoursRan2 = timeRan / 3600000.0;
            double hoursRan = Double.parseDouble(df.format(hoursRan2));
            graphics.setColor(bgColor);
            graphics.draw3DRect(6,342, 490, 131, true);
            graphics.fill3DRect(6, 342, 490, 131, true);
            graphics.setColor(Color.BLACK);
            graphics.drawString("Chicken Master 9000 v"+VERSION, 15, 360);
            graphics.drawString("Time Ran: "+ Timing.msToString(timeRan),15,375);
            graphics.drawString("State: "+sState(), 15, 390);
            graphics.drawString("Levels Gained: "+getLvlsGained(), 15, 405);
            int xpGained = getXpGained();
            graphics.drawString("XP Gained: "+xpGained + " (" + df2.format(xpGained / hoursRan)+ " P/H)", 15 , 420);
            if (lootFeathers) graphics.drawString("Feathers looted: "+looted+ " ("+ df2.format(looted / hoursRan)+ " P/H)", 15, 435);
            if (roundRobin) {
                long nextS = nextSwitchMillis - currentTime;
                graphics.drawString("Time till next switch: "+Timing.msToString(nextS), 15, 450);
            }
        }
    }

    int[] getLvls() {
        int[] lvls = new int[4];
        lvls[0] = Skills.getActualLevel(Skills.SKILLS.ATTACK);
        lvls[1] = Skills.getActualLevel(Skills.SKILLS.STRENGTH);
        lvls[2] = Skills.getActualLevel(Skills.SKILLS.DEFENCE);
        lvls[3] = Skills.getActualLevel(Skills.SKILLS.HITPOINTS);
        return lvls;
    }

    int[] getXp() {
        int[] xps = new int[4];
        xps[0] = Skills.getXP(Skills.SKILLS.ATTACK);
        xps[1] = Skills.getXP(Skills.SKILLS.STRENGTH);
        xps[2] = Skills.getXP(Skills.SKILLS.DEFENCE);
        xps[3] = Skills.getXP(Skills.SKILLS.HITPOINTS);
        return xps;
    }

    int getLvlsGained() {
        int total = 0;
        int[] current = getLvls();
        for (int i = 0; i < startLvls.length; i++) {
            total += current[i] - startLvls[i];
        }
        return total;
    }

    int getXpGained() {
        int total = 0;
        int[] current = getXp();
        for (int i = 0; i < startXP.length; i++) {
            total += current[i] - startXP[i];
        }
        return total;
    }
}

class ChickenGUI extends JFrame implements ActionListener{
    JPanel panel;
    JButton startButton;
    JLabel titleLabel;
    JCheckBox featherBox;
    JCheckBox roundRobin;
    JCheckBox pureBox;
    JLabel animationLabel;
    JTextField animationField;

    boolean started;

    void setup() {
        setSize(275, 175);
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        startButton = new JButton("Start");
        startButton.setSize(50,50);
        startButton.setAlignmentX(CENTER_ALIGNMENT);
        startButton.addActionListener(this);
        titleLabel = new JLabel("Chicken Master 9000: Setup");
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);
        featherBox = new JCheckBox("Loot Feathers");
        featherBox.setAlignmentX(CENTER_ALIGNMENT);
        roundRobin = new JCheckBox("Round Robin");
        roundRobin.setAlignmentX(0.5f);
        pureBox = new JCheckBox("Pure Mode");
        pureBox.setAlignmentX(0.5f);
        animationLabel = new JLabel("Enter Animation");
        animationField = new JTextField("386");

        panel.add(titleLabel);
        panel.add(featherBox);
        panel.add(roundRobin);
        panel.add(pureBox);
        panel.add(animationLabel);
        panel.add(animationField);
        panel.add(startButton);


        add(panel);

        started = false;
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(startButton)) {
            started = true;
            setVisible(false);
        }
    }
}
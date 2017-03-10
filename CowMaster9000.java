package scripts;

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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Random;

/**
 * Created by tim on 2/22/2017.
 */
@ScriptManifest(authors={"Master 9000"}, name = "Cow Master 9000", version = 0.02,category = "Combat", description = "kills cows in lumbridge")
public class CowMaster9000 extends Script implements Painting {
RSTile[] COWTILES = new RSTile[4];
RSTile BANKTILE;
int[] COWS = {2805, 2806, 2807, 2808, 2809};

double version = 0.02;

int[] startLvls = new int[4];
int[] startXps = new int[4];

int AANIMATION, FOOD;
boolean roundRobin, looting;

long startTime, nextSwitch;

int state;

Random r = new Random();


    @Override
    public void run() {
        init();
        do {
            state = getState();

            switch (state) {
                case 0: // not logged in
                    break;
                case 1: // bank
                    bank();
                    break;
                case 2: // in combat, delay
                    cmbDelay();
                    break;
                case 3: // moving delay
                    sleepStill();
                    break;
                case 4: // animating
                    sleep (500);
                    break;
                case 5: // move to cows
                    walkBack();
                    break;
                case 6: // loot
                    loot();
                    break;
                case 7: // rr switch
                    roundRobin();
                    break;
                case 8: // fight
                    fight();
                    break;
            }
        } while (state != -1);
    }

    boolean started = false;
    void init() {
        CowGUI gui = new CowGUI();
        gui.setup();
        while (!gui.started) {
            sleep(1000);
        }
        BANKTILE = new RSTile(3208, 3219, 2);
        COWTILES[0] = new RSTile(3255, 3290, 0);
        COWTILES[1] = new RSTile(3257, 3281, 0);
        COWTILES[2] = new RSTile(3260, 3272, 0);
        COWTILES[3] = new RSTile( 3260, 3261, 0);
        startTime = System.currentTimeMillis();
        startLvls = getLvls();
        startXps = getXps();
        looting = gui.lootBox.isSelected();
        roundRobin = gui.roundRobin.isSelected();
        AANIMATION = Integer.parseInt(gui.animF.getText());
        FOOD = Integer.parseInt(gui.foodF.getText());

        if (roundRobin) roundRobin();
        started = true;
    }

    int getState() {
        RSPlayer me = Player.getRSPlayer();
        RSTile loc = me.getPosition();
        if (Login.getLoginState() != Login.STATE.INGAME) return 0; // not logged in
        if (needBank()) return 1; // need to bank
        if ((me.isInCombat() || animated) && !killed) return 2; // in combat
        if (me.isMoving()) return 3; // moving , should sleep
        if (me.getAnimation() != -1) return 4; // animating, should sleep
        if (roundRobin && needSwitch()) return 7;
        if (needReturn()) return 5; // return to cows
        if (looting && getLoot() != null)  return 6;
        return 8;
    }

    String sState() {
        switch (state) {
            case 0:
                return "Logged Out";
            case 1:
                return "Banking";
            case 2:
                return "Combat Delay";
            case 3:
                return "Moving Delay";
            case 4:
                return "Animating Delay";
            case 5:
                return "Returning To Cows";
            case 6:
                return "Looting";
            case 7:
                return "Switching Styles";
            case 8:
                return "Fighting";
        }
        return "";
    }

    boolean needBank() {
        if (Inventory.isFull() || (Inventory.getCount(FOOD) == 0 && Player.getRSPlayer().getHealthPercent() < 0.5)) {
            return true;
        }
        return false;
    }

    boolean needReturn() {
        RSTile me = Player.getPosition();
        for (RSTile tile : COWTILES) {
            if (tile.distanceTo(me) < 13) return false;
        }
        return true;
    }

    RSGroundItem getLoot() {
        RSGroundItem[] items = GroundItems.find("Cowhide");
        RSTile me = Player.getPosition();
        for (RSGroundItem i : items) {
            if (i != null) {
                if (me.distanceTo(i) < 4) return i;
            }
        }
        return null;
    }

    RSNPC getCow() {
        RSNPC[] cows = NPCs.findNearest(COWS);
        RSTile me = Player.getPosition();
        for (RSNPC n : cows) {
            if (canAttack(n)) return n;
        }
        return null;
    }

    boolean canAttack(RSNPC n) {
        RSTile pos = n.getPosition();
        if (n == null) return false;
        if (n.isInCombat()) return false;
        if (n.getAnimation() != -1) return false;
        if (Player.getPosition().distanceTo(n) >= 10) return false;
        if (pos.getX() < 3253 && pos.getY() < 3272) return false;
        return true;
    }

    boolean needSwitch() {
        if (!roundRobin) return false;
        if (System.currentTimeMillis() > nextSwitch) return true;
        return false;
    }

    Color bgColor = new Color(204, 187, 154);
    int looted = 0;
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
            graphics.draw3DRect(6, 342, 490, 131, true);
            graphics.fill3DRect(6, 342, 490, 131, true);
            graphics.setColor(Color.BLACK);
            graphics.drawString("Cow Master 9000 v"+version, 15, 360);
            graphics.drawString("Time Ran: " + Timing.msToString(timeRan), 15, 375);
            graphics.drawString("State: " + sState(), 15, 390);
            graphics.drawString("Levels Gained: " + getLvlGain(), 15, 405);
            int xpg = getXpGain();
            graphics.drawString("XP Gained: " + xpg + " ("+df2.format(xpg/hoursRan)+" P/H)", 15, 420);
            if (looting) graphics.drawString("Cowhides looted: " + looted + " ("+ df2.format(looted/hoursRan)+" P/H)", 15, 435);
            if (roundRobin) {
                long nextS = nextSwitch - currentTime;
                graphics.drawString("Time till next switch: " + Timing.msToString(nextS), 15, 450);
            }
        }
    }

    void bank() {
        if (!Inventory.isFull()) return;
        RSPlayer me = Player.getRSPlayer();
        if (BANKTILE.distanceTo(me) > 6) {
            WebWalking.walkTo(BANKTILE);
            sleep(1000,2000);

        }

        if(!Banking.isBankScreenOpen()) {
            Banking.openBank();
            sleep(500, 1000);

        }

        if (Banking.isBankScreenOpen()) {
            Banking.depositAllExcept(FOOD);
            sleep(500,1000);
            if (FOOD != -1 && Inventory.getCount(FOOD) == 0)
            Banking.withdraw((r.nextInt(3)+1), FOOD);
            sleep(1000, 1000);
        }

        if (!Inventory.isFull()) {
            Banking.close();
        }
    }

    void walkBack() {
        WebWalking.walkTo(COWTILES[(new Random().nextInt(4))]);
        sleep(500, 1000);
    }

    void loot() {
        RSGroundItem item = getLoot();
        if (item != null) {
            item.click("Take Cowhide");
            lootDelay();
        }
    }

    boolean fight() {
        RSNPC cow = getCow();
        if (cow == null) return false;
        if (!cow.isOnScreen()) {
            Random r = new Random();
            if (r.nextInt(2) == 0) {
                Camera.turnToTile(cow);
                sleep(150,250);
            } else {
                Walking.walkTo(cow);
                sleep(150,250);
            }
        }
        cow.click("Attack Cow");
        killed = false;
        animated = false;
        wtf = 0;
        return preDelay();
    }

    boolean animated = false;
    boolean killed = false;
    int wtf = 0;

    void cmbDelay() {
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
        for (int i = 0; i < 20; i++) {
            RSNPC next = getCow();

            if (factor >= 7 && next != null && getLoot() == null) {
                if (!next.isOnScreen()) Camera.turnToTile(next);
                next.hover();
            }

            if ((pl.isInCombat() || pl.getAnimation() == AANIMATION && i > 0)) i = 0;
            println("Combat Delay: "+i);

            if (pl.getHealthPercent() < 0.4) {
                RSItem[] foods = Inventory.find(FOOD);
                for (RSItem rsi : foods) {
                    if (rsi != null) {
                        rsi.click("Eat");
                        sleep(250);
                        break;
                    }
                }
            }

            if(target.getHealthPercent() == 0.0) {
                println("target is dead");
                killed = true;
                animated = false;
                if (factor < 7) sleep(2000,3500);
                return;
            }
            sleep(100, 125);
        }
        animated = false;
        killed = true;

    }

    void lootDelay() {
        int count = Inventory.getCount("Cowhide");
        for (int i = 0; i < 10; i++) {
            if (Player.isMoving()) i = 0;
            int change = Inventory.getCount("Cowhide") - count;
            if (change > 0) {
                looted += change;
                return;
            }
            sleep(100,125);
        }
    }

    boolean preDelay() {
        RSPlayer pl = Player.getRSPlayer();
        for (int i = 0; i < 15; i++) {
            if (pl == null) return false;
            if (pl.getAnimation() == AANIMATION) {
                animated = true;
                return true;
            }
            if (pl.isMoving()) i--;
            if (pl.isInCombat()) return true;
            sleep(100);
        }
        return false;
    }

    int[] getLvls() {
        int[] lvls = new int[4];
        lvls[0] = Skills.getActualLevel(Skills.SKILLS.ATTACK);
        lvls[1] = Skills.getActualLevel(Skills.SKILLS.STRENGTH);
        lvls[2] = Skills.getActualLevel(Skills.SKILLS.DEFENCE);
        lvls[3] = Skills.getActualLevel(Skills.SKILLS.HITPOINTS);
        return lvls;
    }

    int[] getXps() {
        int[] xps = new int[4];
        xps[0] = Skills.getXP(Skills.SKILLS.ATTACK);
        xps[1] = Skills.getXP(Skills.SKILLS.STRENGTH);
        xps[2] = Skills.getXP(Skills.SKILLS.DEFENCE);
        xps[3] = Skills.getXP(Skills.SKILLS.HITPOINTS);
        return xps;
    }

    int getXpGain() {
        int[] temp = getXps();
        int total = 0;
        for (int i = 0; i < startXps.length; i++) {
            total += (temp[i] - startXps[i]);
        }
        return total;
    }

    int getLvlGain() {
        int[] temp = getLvls();
        int total = 0;
        for (int i = 0; i < startXps.length; i++) {
            total += (temp[i] - startLvls[i]);
        }
        return total;
    }

    void roundRobin() {
        Random r = new Random();
        int secs = r.nextInt(4400) + 500;
        nextSwitch = System.currentTimeMillis() + (secs*1000);
        int style = r.nextInt(3);
        switch (style) {
            case 0: // attack
                println("[ROUND ROBIN] Switching to attack.");
                Combat.selectIndex(0);
                break;
            case 1: // strength
                println("[ROUND ROBIN] Switching to strength.");
                Combat.selectIndex(1);
                break;
            case 2: // def
                println("[ROUND ROBIN] Switching to defense.");
                Combat.selectIndex(3);
                break;
        }
        sleep(500, 1000);
        Inventory.open();
    }

    void sleepStill() {
        sleep(100);
        for (int i = 0; i < 40; i++) {
            if (!Player.isMoving()) {
                sleep(250);
                return;
            }
            if (Player.getAnimation() == AANIMATION)
                animated = true;
            sleep(100);
        }
    }
}
class CowGUI extends JFrame implements ActionListener{
    JPanel panel;
    JButton startButton;
    JLabel titleLabel;
    JCheckBox lootBox;
    JCheckBox roundRobin;
    JLabel animLabel;
    JTextField animF;
    JLabel foodLabel;
    JTextField foodF;


    boolean started;

    void setup() {
        setSize(275, 250);
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        startButton = new JButton("Start");
        startButton.setSize(50,50);
        startButton.setAlignmentX(CENTER_ALIGNMENT);
        startButton.addActionListener(this);
        titleLabel = new JLabel("Cow Master 9000: Setup");
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);
        lootBox = new JCheckBox("Loot");
        lootBox.setAlignmentX(CENTER_ALIGNMENT);
        roundRobin = new JCheckBox("Round Robin");
        roundRobin.setAlignmentX(0.5f);
        animLabel = new JLabel("Animation ID");
        animF = new JTextField("486");
        foodLabel = new JLabel("Food ID");
        foodF = new JTextField("-1");

        panel.add(titleLabel);
        panel.add(lootBox);
        panel.add(roundRobin);
        panel.add(animLabel);
        panel.add(animF);
        panel.add(foodLabel);
        panel.add(foodF);
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

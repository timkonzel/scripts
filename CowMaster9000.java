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
import java.util.Random;

/**
 * Created by tim on 2/22/2017.
 */
@ScriptManifest(authors={"Master 9000"}, name = "Cow Master 9000", version = 0.01,category = "Combat", description = "kills cows in lumbridge")
public class CowMaster9000 extends Script implements Painting {
RSTile[] COWTILES = new RSTile[4];
RSTile BANKTILE;
int[] COWS = {2805, 2806, 2807, 2808, 2809};

double version = 0.01;

int[] startLvls = new int[4];
int[] startXps = new int[4];

boolean roundRobin, looting;

long startTime, nextSwitch;

int state;


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
        if (Inventory.isFull()) {
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
                if (me.distanceTo(i) < 3) return i;
            }
        }
        return null;
    }

    RSNPC getCow() {
        RSNPC[] cows = NPCs.find(COWS);
        RSTile me = Player.getPosition();
        for (RSNPC n : cows) {
            if (n != null && !n.isInCombat() && me.distanceTo(n) < 10) return n;
        }
        return null;
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
            graphics.setColor(bgColor);
            graphics.draw3DRect(6, 342, 490, 131, true);
            graphics.fill3DRect(6, 342, 490, 131, true);
            graphics.setColor(Color.BLACK);
            graphics.drawString("Cow Master 9000 v"+version, 15, 360);
            graphics.drawString("Time Ran: " + Timing.msToString(timeRan), 15, 375);
            graphics.drawString("State: " + sState(), 15, 390);
            graphics.drawString("Levels Gained: " + getLvlGain(), 15, 405);
            graphics.drawString("XP Gained: " + getXpGain(), 15, 420);
            if (looting) graphics.drawString("Cowhides looted: " + looted, 15, 435);
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
            Banking.depositAll();
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
        return preDelay();
    }

    boolean animated = false;
    boolean killed = false;

    void cmbDelay() {
        RSCharacter target = Combat.getTargetEntity();
        if (target == null) {
            println("cmb delay was null");
            sleep(500);
            return;
        }
        RSNPC next = getCow();
        // Start Delay
        for (int i = 0; i < 15; i++) {
            RSPlayer pl = Player.getRSPlayer();
            if (pl.isInCombat() || pl.getAnimation() == 386) i--;
            if (target.getHealthPercent() == 0.0) {

                Random r = new Random();
                if (r.nextInt(10) <= 4) { // Random chance to wait until loot
                    for (int y = 0; y < 15; y++) { // sleeps until our cow drops loot
                        if (target == null) {
                            println("Despawned");
                            return;
                        }
                        sleep(100);
                    }
                }
                killed = true;
                animated = false;
                return;
            }

            // MOUSE HOVERING ON NEXT TARGET
            if(next != null) {
                if (next.isOnScreen() && !next.isInCombat()) {
                    next.hover();
                } else {
                    next = getCow();
                }
            } else {
                next = getCow();
            }
            sleep(100);
        }
        animated = false;

    }

    void lootDelay() {
        int count = Inventory.getCount("Cowhide");
        for (int i = 0; i < 10; i++) {
            if (Player.isMoving()) i--;
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
            if (pl.getAnimation() == 386) {
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
            if (Player.getAnimation() == 386)
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

    boolean started;

    void setup() {
        setSize(275, 175);
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

        panel.add(titleLabel);
        panel.add(lootBox);
        panel.add(roundRobin);
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

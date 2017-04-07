package scripts;

import org.tribot.api.Timing;
import org.tribot.api.input.Mouse;
import org.tribot.api.util.abc.ABCUtil;
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
@ScriptManifest(authors={"Master 9000"}, name = "Cow Master 9000", version = 0.03,category = "Combat", description = "kills cows in lumbridge")
public class CowMaster9000 extends Script implements Painting {
RSTile[] COWTILES = new RSTile[4];
RSTile BANKTILE;
int[] COWS = {2805, 2806, 2807, 2808, 2809};

double version = 0.03;

int[] startLvls = new int[5];
int[] startXps = new int[5];

int AANIMATION, FOOD, ARROW_ID;
String ARROW_NAME;
boolean looting, ranging;
int SPOT;

long startTime;

int state;
int equipArrowAmt = 0;

ABCUtil abc2;
int runAt;
boolean shouldHover, shouldMenu;

Random r = new Random();

    @Override
    public void run() {
        init();
        do {
            state = getState();
            abc2TimedActions();
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
        ranging = gui.ranging.isSelected();
        AANIMATION = Integer.parseInt(gui.animF.getText());
        FOOD = Integer.parseInt(gui.foodF.getText());
        int aIndex = gui.arrowBox.getSelectedIndex();
        switch(gui.arrowBox.getSelectedIndex()) {
            case 0: // none
                ARROW_ID = -1;
                ARROW_NAME = "";
                break;
            case 1: // bronze arrow
                ARROW_ID = 882;
                ARROW_NAME = "Bronze Arrow";
                break;
            case 2: // iron arrow
                ARROW_ID = 884;
                ARROW_NAME = "Iron Arrow";
                break;
            case 3: // steel arrow
                ARROW_ID = 886;
                ARROW_NAME = "Steel Arrow";
                break;
            case 4: // bone bolts
                ARROW_ID = 8882;
                ARROW_NAME = "Bone Bolt";
                break;
            default:
                ARROW_ID = -1;
                ARROW_NAME = "";
                break;
        }
        SPOT = gui.locationBox.getSelectedIndex();
        switch (SPOT) {
            case 0: // no location
                COWTILES = null;
                break;
            case 1: // lumbridge east (main spot)
                COWTILES[0] = new RSTile(3255, 3290, 0);
                COWTILES[1] = new RSTile(3257, 3281, 0);
                COWTILES[2] = new RSTile(3260, 3272, 0);
                COWTILES[3] = new RSTile( 3260, 3261, 0);
                break;
            case 2: // lumbridge west
                COWTILES[0] = new RSTile(3203, 3291, 0);
                COWTILES[1] = new RSTile(3203, 3291, 0);
                COWTILES[2] = new RSTile(3203, 3291, 0);
                COWTILES[3] = new RSTile( 3203, 3291, 0);
                break;
            case 3: // falador (no yet added)
                BANKTILE = new RSTile(3012, 3356, 0);
                COWTILES[0] = new RSTile(3031, 3306, 0);
                COWTILES[1] = new RSTile(3031, 3306, 0);
                COWTILES[2] = new RSTile(3031, 3306, 0);
                COWTILES[3] = new RSTile( 3031, 3306, 0);
                break;
            default:
                break;

        }
        started = true;
        abc2 = new ABCUtil();
        runAt = abc2.generateRunActivation();
    }

    int getState() {
        RSPlayer me = Player.getRSPlayer();
        RSTile loc = me.getPosition();
        if (Login.getLoginState() != Login.STATE.INGAME) return 0; // not logged in
        if (needBank()) return 1; // need to bank
        if ((me.isInCombat() || animated) && !killed) return 2; // in combat
        if (me.isMoving()) return 3; // moving , should sleep
        if (me.getAnimation() != -1) return 4; // animating, should sleep
        if (needReturn()) return 5; // return to cows
        if (needLoot())  return 6;
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
        if ((Inventory.isFull() && Inventory.getCount(ARROW_ID) == 0) || (Inventory.getCount(FOOD) == 0 && Player.getRSPlayer().getHealthPercent() < 0.5)) {
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

    boolean needLoot() {
        if (ranging && getArrows() != null) return true;
        if (!looting) return false;
        if (getLoot() != null) return true;
        return false;
    }

    RSGroundItem getLoot() {
        RSGroundItem[] items = GroundItems.findNearest("Cowhide");
        RSTile me = Player.getPosition();
        for (RSGroundItem i : items) {
            if (i != null) {
                if (me.distanceTo(i) < 4) return i;
            }
        }
        return null;
    }

    RSGroundItem getArrows() {
        RSGroundItem[] arrows = GroundItems.find(ARROW_ID);

        for (RSGroundItem item : arrows) {
            if (item != null && item.getStack() >  3 && Player.getPosition().distanceTo(item) < 4)
                return item;
        }
        return null;
    }

    RSNPC getCow() {
        RSNPC[] cows = NPCs.findNearest(COWS);
        for (RSNPC n : cows) {
            if (canAttack(n)) return n;
        }
        return null;
    }

    boolean canAttack(RSNPC n) {
        if (n == null) return false;
        if (n.isInCombat()) return false;
        if (n.getInteractingCharacter() != null) return false;
        if (Player.getPosition().distanceTo(n) >= 10) return false;
        if (!inArea(n.getPosition())) return false;
        return true;
    }

    boolean inArea(RSTile loc) {
        switch(SPOT) {
            case 0: // no location set
                return false;
            case 1: // lumbridge east
                if (loc.getX() < 3253 || loc.getY() < 3272) return false;
                return true;
            case 2: // lumbridge west
                if (loc.getY() < 3283 || loc.getY() > 3301 || loc.getX() < 3195 || loc.getX() > 3210) return false;
                return true;
            case 3: // falador
                if (loc.getY() < 3298 || loc.getY() > 3313 || loc.getX() < 3021 || loc.getX() > 3043) return false;
                return true;
            default:
                return false;
        }
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
        equip();
        RSGroundItem arrows = getArrows();
        RSTile myLoc = Player.getPosition();
        if (arrows != null) {
            RSTile arrowLoc = arrows.getPosition();
            /*
            * this is to make sure we dont get caught trying to loot
            * when an item is outside of fence and we are in / vice versa
             */
            if(inArea(arrowLoc) != inArea(myLoc)) {
                println("Web walking to loot to prevent possible bot-like actions");
                WebWalking.walkTo(arrows);
            }
            arrows.click("Take "+ARROW_NAME);
            lootDelay();
            return;
        }
        if (looting) {
            RSGroundItem item = getLoot();
            if (item != null) {
                RSTile lootLoc = item.getPosition();
                if (inArea(myLoc) != inArea(lootLoc)) {
                    println("Web walking to loot to prevent possible bot-like actions");
                    WebWalking.walkTo(item);
                }
                item.click("Take Cowhide");
                lootDelay();
            }
        }
    }

    void abc2TimedActions() {
        if (abc2.shouldLeaveGame()) {
            println("ABC2: Leaving Game");
            abc2.leaveGame();
            return;
        }
        if (abc2.shouldCheckTabs()) {
            println("ABC2: Checking Tabs");
            abc2.checkTabs();
            return;
        }
        if (abc2.shouldCheckXP()) {
            println("ABC2: Checking XP");
            abc2.checkXP();
            return;
        }
        if (abc2.shouldExamineEntity()) {
            println("ABC2: Examine Entity");
            abc2.examineEntity();
            return;
        }

        if (abc2.shouldMoveMouse()) {
            println("ABC2: Moving Mouse");
            abc2.moveMouse();
            return;
        }
        if (abc2.shouldPickupMouse()) {
            println("ABC2: Pickup Mouse");
            abc2.pickupMouse();
            return;
        }
        if (abc2.shouldRightClick()) {
            println("ABC2: Right Click");
            abc2.rightClick();
            return;
        }
        if (abc2.shouldRotateCamera()) {
            println("ABC2: Rotate Camera");
            abc2.rotateCamera();
            return;
        }

        // i lied the others are going here too
        if (!Game.isRunOn() && Game.getRunEnergy() >= runAt) {
            Options.setRunOn(true);
            runAt = this.abc2.generateRunActivation();
        }
    }

    boolean equip() {
        if (!ranging) return false;
        if (Inventory.getCount(ARROW_ID) > equipArrowAmt || Inventory.isFull() || Equipment.getItem(Equipment.SLOTS.ARROW) == null) {
            RSItem[] item = Inventory.find(ARROW_ID);
            println("Equipping arrows");
            if (item[0] != null) {
                item[0].click("Wield");
                sleep(80,350);
                return true;
            }
        }
        return false;
    }

    boolean fight() {
        if (equip()) equipArrowAmt = r.nextInt(100);
        RSNPC cow = getCow();
        RSTile me = Player.getPosition();
        if (cow == null) return false;
        if (inArea(me) != inArea(cow.getPosition())) { // walks us to cow if it is possibly blocked by a gate
            println("Web walking to cow to prevent possible bot-like actions");
            WebWalking.walkTo(cow);
        }
        if (!cow.isOnScreen()) {
            if (r.nextInt(2) == 0) {
                Camera.turnToTile(cow);
                sleep(150, 250);
            } else {
                Walking.walkTo(cow);
                sleep(150, 250);
            }
        }
        cow.click("Attack Cow");
        killed = false;
        animated = false;
        wtf = 0;
        shouldHover = abc2.shouldHover();
        shouldMenu = abc2.shouldOpenMenu();
        return preDelay();
    }

    boolean animated = false;
    boolean killed = false;
    int wtf = 0;

    void cmbDelay() {
        RSCharacter target = Combat.getTargetEntity();
        if (target == null) {
            sleep(200,400);
            //println("target is null");
            wtf++;
            if (wtf > 8) {
                killed = true;
                animated = false;
            }
            return;
        }

       // println("target is real");
        RSPlayer pl = Player.getRSPlayer();
        RSNPC next = getCow();
        for (int i = 0; i < 20; i++) {
            /*
            *   update our next target
            *   if our previous next target is no longer a valid target
             */
            if (!canAttack(next)) next = getCow();

            /*
            *   hover over our next target if we arent
            *   only do so if ABC2 Says so & we are not about to loot
             */
            if (shouldHover && next != null && !needLoot() && !isHovering(next) && !ChooseOption.isOpen()) {
                if (!next.isOnScreen()) Camera.turnToTile(next);
                println("ABC2: Hover Next Target");
                next.hover();
            }
            if (shouldMenu && next != null && !needLoot() && isHovering(next) && !ChooseOption.isOpen()) {
                println("ABC2: Open Next Menu");
                Mouse.click(3);
            }

            if ((pl.isInCombat() || pl.getAnimation() == AANIMATION && i > 0)) i = 0; // reset sleep
            RSCharacter check = Combat.getTargetEntity();
            if (check != null && !target.equals(check)) {
                target = check;
            }

            if (pl.getHealthPercent() < 0.4 && FOOD != -1) { // we need to eat or get the hell out
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
                if(ChooseOption.isOpen()) {
                    ChooseOption.select("Attack Cow");
                    sleepStill();
                    i = 0;
                } else {
                    killed = true;
                    animated = false;
                    if (!shouldHover && r.nextInt(10) >= 7) sleep(2000,3500); // randomly wait for loot
                    return;
                }
            }

            sleep(100, 125);
            abc2TimedActions();
        }
        animated = false;
        killed = true;

    }

    boolean isHovering(RSNPC n) {
        if (n == null || n.getAnimablePosition() == null || n.getAnimablePosition().getHumanHoverPoint() == null || Mouse.getPos() == null) return false;
        Point p1 = n.getAnimablePosition().getHumanHoverPoint();
        Point p2 = Mouse.getPos();
        int dist = Math.abs(p2.x - p1.x) + Math.abs(p2.y - p1.y);
        if (dist < 60) {
            return true;
        }
        return false;
    }

    void lootDelay() {
        int count = Inventory.getCount("Cowhide");
        int count2 = Inventory.getCount(ARROW_ID);
        for (int i = 0; i < 10; i++) {
            if (Player.isMoving()) i = 0;
            int change = Inventory.getCount("Cowhide") - count;
            int change2 = Inventory.getCount(ARROW_ID) - count2;
            if (change > 0) {
                looted += change;
                return;
            }
            if (change2 > 0) {
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
        int[] lvls = new int[5];
        lvls[0] = Skills.getActualLevel(Skills.SKILLS.ATTACK);
        lvls[1] = Skills.getActualLevel(Skills.SKILLS.STRENGTH);
        lvls[2] = Skills.getActualLevel(Skills.SKILLS.DEFENCE);
        lvls[3] = Skills.getActualLevel(Skills.SKILLS.HITPOINTS);
        lvls[4] = Skills.getActualLevel(Skills.SKILLS.RANGED);
        return lvls;
    }

    int[] getXps() {
        int[] xps = new int[5];
        xps[0] = Skills.getXP(Skills.SKILLS.ATTACK);
        xps[1] = Skills.getXP(Skills.SKILLS.STRENGTH);
        xps[2] = Skills.getXP(Skills.SKILLS.DEFENCE);
        xps[3] = Skills.getXP(Skills.SKILLS.HITPOINTS);
        xps[4] = Skills.getXP(Skills.SKILLS.RANGED);
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
            abc2TimedActions();
        }
    }
}
class CowGUI extends JFrame implements ActionListener{
    String[] locations = {"Select a location", "Lumbridge East", "Lumbridge West", "Falador"};
    String[] arrows = {"- Select One -", "Bronze Arrow", "Iron Arrow", "Steel Arrow", "Bone Bolts"};
    JPanel panel;
    JButton startButton;
    JLabel titleLabel;
    JCheckBox lootBox;
    JLabel animLabel;
    JTextField animF;
    JLabel foodLabel;
    JTextField foodF;
    JCheckBox ranging;
    JComboBox locationBox, arrowBox;

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
        animLabel = new JLabel("Animation ID");
        animLabel.setAlignmentX(0.5f);
        animF = new JTextField("486");
        animF.setAlignmentX(0.5f);
        foodLabel = new JLabel("Food ID");
        foodLabel.setAlignmentX(0.5f);
        foodF = new JTextField("-1");
        foodF.setAlignmentX(0.5f);
        ranging = new JCheckBox("Ranging");
        ranging.setAlignmentX(0.5f);
        locationBox = new JComboBox(locations);
        locationBox.setAlignmentX(0.5f);
        arrowBox = new JComboBox(arrows);
        arrowBox.setAlignmentX(0.5f);


        panel.add(titleLabel);
        panel.add(locationBox);
        panel.add(lootBox);
        panel.add(ranging);
        panel.add(arrowBox);
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

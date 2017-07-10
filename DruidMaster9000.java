package scripts;

import obf.JL;
import org.tribot.api.Timing;
import org.tribot.api2007.*;
import org.tribot.api2007.Objects;
import org.tribot.api2007.types.*;
import org.tribot.script.Script;
import org.tribot.script.ScriptManifest;
import org.tribot.script.interfaces.Painting;
import scripts.webwalker_logic.WebWalker;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by tim on 3/12/2017.
 */
@ScriptManifest(authors={"Master 9000"}, name = "Druid Master 9000", version = 0.01,category = "Combat", description = "multiple location druid killer")
public class DruidMaster9000 extends Script implements Painting {
    double version = 0.01;
    DruidGUI gui;
    boolean looting;
    Random r = new Random();
    // SCRIPT SETTINGS
    RSTile BANK_TILE;
    RSTile DRUID_LOC;

   // RSTile LADDER_TILE_TOP = new RSTile(3094, 3471);
    // RSTile LADDER_TILE_BOT = new RSTile(3096, 9876);

    int[] DRUID_IDS = {2878};
    int[] LOOT;
    int FOOD_ID, ANIMATION_ID, FOOD_AMT;

    int state;

    int TELEPORT_IN_ID = -1;
    int TELEPORT_OUT_ID = -1;
    int LOCATION = 0;

    int[] looted = new int[20];

    boolean killed = false;
    boolean animated = false;
    boolean needHop = false;
    boolean HOPPING = false;
    int nullTimer = 0;

    int[] startLvls = new int[5];
    int[] startXps = new int[5];

    private void init() {
        gui = new DruidGUI();
        gui.setup();
        while (!gui.started) {
            sleep(200);
        }
        started = true;
        startTime = System.currentTimeMillis();
        startXps = getXps();
        startLvls = getLvls();
        BANK_TILE = new RSTile(2946, 3368);
        DRUID_LOC = new RSTile(2933, 9848);

        ArrayList<Integer> lootids = new ArrayList<Integer>();
        if (gui.guamBox.isSelected()) lootids.add(199);
        if (gui.marrentillBox.isSelected()) lootids.add(201);
        if (gui.tarrominBox.isSelected()) lootids.add(203);
        if (gui.harralanderBox.isSelected()) lootids.add(205);
        if (gui.ranarrBox.isSelected()) lootids.add(207);
        if (gui.iritBox.isSelected()) lootids.add(209);
        if (gui.avantoeBox.isSelected()) lootids.add(211);
        if (gui.kwuarmBox.isSelected()) lootids.add(213);
        if (gui.cadantineBox.isSelected()) lootids.add(215);
        if (gui.dwarfBox.isSelected()) lootids.add(217);
        if (gui.lantadymeBox.isSelected()) lootids.add(2485);
        if (gui.earthBox.isSelected()) lootids.add(557);
        if (gui.bodyBox.isSelected()) lootids.add(559);
        if (gui.mindBox.isSelected()) lootids.add(558);
        if (gui.lawBox.isSelected()) lootids.add(563);
        if (gui.natureBox.isSelected()) lootids.add(561);
        if (gui.boltBox.isSelected()) lootids.add(9142);
        if (gui.airBox.isSelected()) lootids.add(556);
        if (gui.rareBox.isSelected()) {
            //Uncut Sapphire
            //Uncut emerald
            //Uncut ruby
            //Uncut diamond
            //Dragonstone
            //Runite Bar
            //Silver ore
            //loop half
            //tooth half
            //rune 2h
            //rune b-axe
            //rune kite
            //d med
            //rune spear
            //shield left
            //d spear
        }
        int ind = lootids.size();
        if (ind > 0) {
            looting = true;
            LOOT = new int[ind];
            for (int i = 0; i < ind; i++) {
                LOOT[i] = lootids.get(i);
            }
        }  else {
            looting = false;
        }

        ANIMATION_ID = Integer.parseInt(gui.animA.getText());
        switch (gui.foodBox.getSelectedIndex()) {
            case 0: // none
                break;
            case 1: // trout
                FOOD_ID = 333;
                break;
            case 2: // lobster
                FOOD_ID = 379;
                break;
        }
        switch (gui.location.getSelectedIndex()) {
            case 0: // tavelry dung
                BANK_TILE = new RSTile(2946, 3368);
                DRUID_LOC = new RSTile(2933, 9848);
                LOCATION = 1;
                break;
            case 1: // edge dungeon
                BANK_TILE = new RSTile(3093, 3491);
                DRUID_LOC = new RSTile(3112, 9932);
                LOCATION = 2;
                break;
        }
        if(gui.teleportIn.getSelectedIndex()==1) TELEPORT_IN_ID = 12781;
        switch(gui.teleportOut.getSelectedIndex()) {
            case 0: // nothing
                break;
            case 1: // falador teleport
                BANK_TILE = new RSTile(2946, 3368);
                TELEPORT_OUT_ID = 8009;
                //LOCATION = 1;
                break;
            case 2: // varrock teleport
                BANK_TILE = new RSTile(3185, 3436);
                TELEPORT_OUT_ID = 8007;
               // LOCATION = 2;
                break;
        }
        FOOD_AMT = gui.foodAMT.getValue();
    }

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
                    cmbDelay2();
                    break;
                case 3: // moving delay
                    movingDelay();
                    break;
                case 4: // animating
                    sleep (500);
                    break;
                case 5: // move to druids
                    walkBack();
                    break;
                case 6: // loot
                    loot();
                    break;
                case 7: // rr switch
                    //roundRobin();
                    break;
                case 8: // fight
                    if (fight()) cmbDelay2();
                    break;
            }
        } while (state != -1);
    }

    public int getState() {
        RSPlayer me = Player.getRSPlayer();
        RSTile loc = me.getPosition();
        if (Login.getLoginState() != Login.STATE.INGAME) return 0; // not logged in
        if (needExit()) manualBank = true;
        if (needBank()) return 1; // need to bank
        if (needLoot())  return 6;
        if (me.isMoving()) return 3; // moving , should sleep
        if (needReturn()) return 5; // return to druids
        if ((me.isInCombat() && System.currentTimeMillis() - lastKill > 3000) || me.getAnimation() != -1) return 2; // in combat
       // if (roundRobin && needSwitch()) return 7;
        return 8;
    }
    boolean manualBank = false;
    // CHECK FUNCTIONS
    boolean needBank() {
        if (manualBank) return true;
        int foodCount = Inventory.getCount(FOOD_ID);
        if (Inventory.isFull() && foodCount == 0) return true;
        if (getMissingHp() > 20 && foodCount == 0) return true;
        return false;
    }

    int getMissingHp() {
        int cur = Skills.getCurrentLevel(Skills.SKILLS.HITPOINTS);
        int actual = Skills.getActualLevel(Skills.SKILLS.HITPOINTS);
        return actual - cur;
    }

    boolean needReturn() {
        if (Player.getPosition().distanceTo(DRUID_LOC) > 10) return true;
        return false;
    }

    boolean needLoot() {
        if (!looting) return false;

        if (getLoot() != null) return true;

        return false;
    }

    boolean canAttack(RSNPC npc) {
        if (npc == null) return false;
        RSTile pos = npc.getPosition();
        if (npc.isInCombat()) return false;
        if (Player.getPosition().distanceTo(npc) > 9) return false;

        return true;
    }

    boolean inBounds(RSTile loc) {
        switch (LOCATION) {
            case 1: // Tavelry
                return true;
            case 2: // Edgeville
                if (loc.getY() <= 9944) return true;
                return false;
            default:
                return false;
        }
    }

    //GET FUNCTIONS
    RSGroundItem getLoot() {
        RSTile me = Player.getPosition();
        RSGroundItem[] items = GroundItems.findNearest(LOOT);
        for (RSGroundItem item : items) {
            if (item != null && me.distanceTo(item) < 7 && inBounds(item.getPosition())) return item;
        }
        return null;
    }

    RSNPC getDruid() {
        RSNPC[] druids = NPCs.findNearest(DRUID_IDS);
        RSCharacter atkr = getAttacker();
        if (atkr != null) return (RSNPC) atkr;
        for (RSNPC n : druids) {
            if (canAttack(n) && inBounds(n.getPosition())) return n;
        }
        return null;
    }

    RSCharacter getAttacker() {
        RSCharacter[] npcs = NPCs.findNearest(DRUID_IDS);
        RSCharacter me = Player.getRSPlayer();


        for (RSCharacter rsc : npcs) {
            if (rsc != null) {
                RSCharacter inter = rsc.getInteractingCharacter();
                if (inter != null && inter.equals(me) || rsc.isInteractingWithMe()) {
                    return rsc;
                }
            }

        }
        return null;
    }

    String getItemName(int id) {
        switch(id) {
            case 199: return "Grimy guam leaf";
            case 201: return "Grimy marrentill";
            case 203: return "Grimy tarromin";
            case 205: return "Grimy harralander";
            case 207: return "Grimy ranarr weed";
            case 209: return "Grimy irit leaf";
            case 211: return "Grimy avantoe";
            case 213: return "Grimy kwuarm";
            case 215: return "Grimy cadantine";
            case 217: return "Grimy dwarf weed";
            case 2485: return "Grimy lantadyme";
            case 557: return "Earth rune";
            case 559: return "Body rune";
            case 558: return "Mind rune";
            case 563: return "Law rune";
            case 561: return "Nature rune";
            case 556: return "Air rune";
            case 9142: return "Mithril bolts";
            default : return "";
        }
    }

    // DO FUUNCTIONS & SLEEPS

    void bank() {

        RSPlayer me = Player.getRSPlayer();
        if (BANK_TILE.distanceTo(me) > 6) {
            if(!needTeleportOut()) {
                teleportOut();
                sleep(3000);
            }
            WebWalker.walkTo(BANK_TILE);
            sleep(1000,2000);

        }

        if(!Banking.isBankScreenOpen()) {
            Banking.openBank();
            sleep(500, 1000);

        }

        if (Banking.isBankScreenOpen()) {
            Banking.depositAll();
            sleep(500,1000);
            if (needTeleportOut())
                Banking.withdraw(1, TELEPORT_OUT_ID);
            if (needTeleportIn())
                Banking.withdraw(1, TELEPORT_IN_ID);
            if (FOOD_ID != -1 && Inventory.getCount(FOOD_ID) == 0)
                Banking.withdraw(FOOD_AMT, FOOD_ID);
            sleep(1000, 1000);
            manualBank = false;
        }

        if (!Inventory.isFull() || Inventory.getCount(FOOD_ID) != 0) {
            Banking.close();
        }

        if (needHop && HOPPING) {
            println("hopping world to avoid pkers");
            WorldHopper.changeWorld(WorldHopper.getRandomWorld(true));
            needHop = false;
        }
    }

    void teleportOut() {
        if (TELEPORT_OUT_ID != -1) {
            RSItem[] teles = Inventory.find(TELEPORT_OUT_ID);
            for (RSItem rsi : teles) {
                if (rsi != null) {
                    rsi.click("Break");
                    return;
                }
            }
        }
    }

    boolean needTeleportOut() {
        if (TELEPORT_OUT_ID != -1 && Inventory.getCount(TELEPORT_OUT_ID) == 0) return true;
        return false;
    }

    boolean needTeleportIn() {
        if (TELEPORT_IN_ID != -1 && Inventory.getCount(TELEPORT_OUT_ID) == 0) return true;
        return false;
    }

    void loot() {
        RSGroundItem item = getLoot();
        if (item != null) {
            if (Inventory.isFull() && Inventory.getCount(FOOD_ID) > 0 && !hasStack(item.getID())) {
                RSItem[] food = Inventory.find(FOOD_ID);
                for (RSItem rsi : food) {
                    if (rsi != null) {
                        rsi.click("Eat");
                        break;
                    }
                }
            }
            if (Player.getPosition().distanceTo(item) > 5)
                Walking.walkTo(item);
            if (!item.isOnScreen()) {
                Camera.turnToTile(item);
            }
            item.click("Take "+getItemName(item.getID()));
            lootDelay(item.getID());
        }
    }

    boolean hasStack(int id) {
        int amt = Inventory.getCount(id);

        if (amt <= 0) return false;
        if (id == 557) return true;
        if (id == 559) return true;
        if (id == 558) return true;
        if (id == 563) return true;
        if (id == 561) return true;
        if (id == 556) return true;
        if (id == 9142) return true;
        return false;
    }

    void lootDelay(int id) {
        int count = Inventory.getCount(id);
        for (int i = 0; i < 10; i++) {
            if (Player.isMoving()) i = 0;
            int change = Inventory.getCount(id) - count;
            if (change > 0) {
                looted[getLootIndex(id)] += change;
                return;
            }
            sleep(100,125);
        }
    }

    int getLootIndex(int id) {
        switch (id) {
            case 199: return 0; // guam
            case 201: return 1; // marrentill
            case 203: return 2; // tarromin
            case 205: return 3; // harralander
            case 207: return 4; // ranarr
            case 209: return 5; // irit
            case 211: return 6; // avantoe
            case 213: return 7; // kwuarm
            case 215: return 8; // cadantine
            case 217: return 9; // dwarf
            case 219: return 10; // lantadyme
            case 556: return 11; // air rune
            case 557: return 12; // earth rune
            case 558: return 13; // mind rune
            case 559: return 14; // body rune
            case 561: return 15; // nature rune
            case 563: return 16; // law rune
            case 9142: return 17; // mithril bolts
            default: return 0;
        }
    }

    int getPrice(int lootIndex) {
        switch (lootIndex) {
            case 0: return 7; // guam
            case 1: return 13; // marr
            case 2: return 115; // tarr
            case 3: return 460; // harr
            case 4: return 7090; // ran
            case 5: return 1041; // irit
            case 6: return 2072; // avantoe
            case 7: return 2228; // kwuarm
            case 8: return 1757; // cadantine
            case 9: return 961; // cadantine
            case 10: return 1935; // lantadyme
            case 11: return 5; // air rune
            case 12: return 5; // earth rune
            case 13: return 4; // mind rune
            case 14: return 11; // body rune
            case 15: return 232; // nature rune
            case 16: return 173; // law rune
            case 17: return 88;
            default: return 1;
        }
    }

    boolean fight() {
        RSNPC druid = getDruid();
        if (!Game.isRunOn() && Game.getRunEnergy() > 30) Options.setRunOn(true);
        if (druid == null) return false;
        if (!druid.isOnScreen()) {
            if (r.nextInt(2) == 0) {
                Camera.turnToTile(druid);
                sleep(150,250);
            } else {
                Walking.walkTo(druid);
                sleep(150,250);
            }
        }

        druid.click("Attack Chaos Druid");
        killed = false;
        animated = false;
        nullTimer = 0;
        return preDelay();
    }

    boolean preDelay() {
        RSPlayer pl = Player.getRSPlayer();
        for (int i = 0; i < 10; i++) {
            if (pl == null) return false;
            if (pl.getAnimation() != -1) {
                animated = true;
                lastAnim = System.currentTimeMillis();
                return true;
            }
            if (pl.isMoving()) i = 0;
            if (pl.isInCombat()) return true;
            sleep(100);
        }
        return false;
    }

    long lastAnim = 0;
    long lastKill = 0;
    void cmbDelay2() {
        state = 2;
        RSCharacter target = Player.getRSPlayer().getInteractingCharacter();
        RSCharacter attacker = getAttacker();
        for (int i = 0; i < 15; i++) {

            // eat if low hp
            if (getMissingHp() > 20 && FOOD_ID != -1) {
                if (Inventory.getCount(FOOD_ID) == 0) return;
                RSItem[] foods = Inventory.find(FOOD_ID);
                for (RSItem rsi : foods) {
                    if (rsi != null) {
                        rsi.click("Eat");
                        sleep(750,1250);
                        return;
                    }
                }
            }


            if (LOCATION == 2 && needExit()){
                println("detected a pker, teleporting out");
                manualBank = true;
                break;
            }

            if (needLoot()) // loot if needed
                break;
            if (Player.getAnimation() == ANIMATION_ID) lastAnim = System.currentTimeMillis();

            //update the target and attacker
            if (attacker == null) attacker = getAttacker();
            if (target == null) target = Player.getRSPlayer().getInteractingCharacter();


            if (target != null && target.getHealthPercent() == 0.0) {
                println("target is dead");
                lastKill = System.currentTimeMillis();
                fight(); // try to fight new targ
                return;
            }

            if (attacker != null && attacker.getHealthPercent() == 0.0) {
                println("attacker is dead");
                fight(); // try to fight
                return;
            }

            if (Player.getRSPlayer().isInCombat()) {
                i = 0;
                if (System.currentTimeMillis() - lastAnim > 3000 && System.currentTimeMillis() - lastKill > 3000) {
                    println("we are in combat and haven't attacked in a while, attempting to re-attack");
                    RSCharacter[] attacker2 = Combat.getAttackingEntities();
                    attacker = getAttacker();
                    if (attacker != null) {
                        attacker.click("Attack");
                    }
                }
            }

            sleep(100);
        }
    }

    void movingDelay() {
        for (int i = 0; i < 40; i++) {
            if (!Player.isMoving()) {
                return;
            }
            if (Player.getAnimation() == ANIMATION_ID)
                animated = true;
            sleep(100);
        }
    }


    long ladder = 0;
    void walkBack() {
        if (!needTeleportIn()) {
            teleportIn();
        }

        Camera.setCameraRotation(270);
        Camera.setCameraAngle(100);
        WebWalker.walkTo(DRUID_LOC);
        sleep(200);
        }

    void teleportIn() {
        if(TELEPORT_IN_ID == 12781) {
            RSItem[] teles = Inventory.find(TELEPORT_IN_ID);
            for (RSItem rsi : teles) {
                if (rsi != null) {
                    rsi.click("Break");
                    sleep(200);
                    return;
                }
            }
        }
    }

    boolean needExit() {
        int wildLvl = Combat.getWildernessLevel();
        int myCb = Player.getRSPlayer().getCombatLevel();
        if (Player.getPosition().distanceTo(DRUID_LOC) < 11) {
            RSPlayer[] players = Players.getAll();
            for (RSPlayer pl : players) {
                if (pl != null && !pl.equals(Player.getRSPlayer())) {
                    int diff = pl.getCombatLevel()-myCb;
                    int lvl = getWildLvl(Player.getPosition());
                    if (Math.abs(diff) <= lvl && lvl > 0) {
                        println("need to teleport due to: "+pl.getName()+" cb:"+pl.getCombatLevel() + " wildy lvl: "+ lvl);
                        needHop = true;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    int getWildLvl(RSTile tile) {
        if (tile == null) return 0;
        if (tile.getY() >= 9944 ) return 4;
        if (tile.getY() >= 9936 ) return 3;
        if (tile.getY() >= 9928 ) return 2;
        if (tile.getY() >= 9920 ) return 1;
        return 0;
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
                return "Returning To Druids";
            case 6:
                return "Looting";
            case 7:
                return "Switching Styles";
            case 8:
                return "Fighting";
        }
        return "";
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

    int getLootTotal() {
        int price;
        int total = 0;
        for(int i = 0; i < looted.length; i++) {
            total += looted[i]*getPrice(i);
        }
        return total;
    }

    long startTime;
    Color bgColor = new Color(204, 187, 154);
    boolean started = false;
    boolean debug = true;
    private Image getImage(String url) {
        try {
            return ImageIO.read(new URL(url));
        } catch(IOException e) {
            return null;
        }
    }
    private final Image img = getImage("http://i.imgur.com/RTDARLx.gif");
    @Override
    public void onPaint(Graphics graphics) {
        if (started) {
            Graphics2D g2 = (Graphics2D) graphics;
            g2.drawImage(img, 1, 338, null);
            long currentTime = System.currentTimeMillis();
            long timeRan = currentTime - startTime;
            DecimalFormat df = new DecimalFormat("##.####");
            DecimalFormat df2 = new DecimalFormat("####.##");
            df.setRoundingMode(RoundingMode.DOWN);
            double hoursRan2 = timeRan / 3600000.0;
            double hoursRan = Double.parseDouble(df.format(hoursRan2));
            graphics.setColor(Color.green);
            graphics.drawString("v"+version, 7, 498);
            graphics.drawString(Timing.msToString(timeRan), 186, 415);
            graphics.drawString("" + getLvlGain(), 186, 440);
            int xpg = getXpGain();
            graphics.drawString(xpg + " ("+df2.format(xpg/hoursRan)+" P/H)", 186, 462);
            graphics.drawString(getLootTotal() + " ("+ df2.format(getLootTotal()/hoursRan)+" P/H)", 186, 487);
            graphics.drawString("State: " + sState(), 370, 330);
           /* graphics.setColor(Color.orange);
            RSCharacter attacker = getAttacker();
            if (attacker != null) {
                Point pt = attacker.getAnimablePosition().getHumanHoverPoint();
                graphics.drawRect(pt.x,pt.y,20,20);
            }
            RSNPC targ = getDruid();
            graphics.setColor(Color.red);
            if (targ != null) {
                Point pt = targ.getAnimablePosition().getHumanHoverPoint();
                graphics.drawRect(pt.x,pt.y,20,20);
            }*/
        }
    }
}
class DruidGUI extends JFrame implements ActionListener {
    boolean started = false;

    //labels & text areas
    JLabel titleLabel = new JLabel("Druid Master 9000");
    JLabel foodLabel = new JLabel("Food ID");
    JLabel animationLabel = new JLabel("Animation ID");
    JLabel toutLabel = new JLabel("Teleport Out");
    JLabel tinLabel = new JLabel("Teleport In");
    JLabel locationLabel = new JLabel("Location");
    JLabel amountLabel = new JLabel("Amount");
    JLabel emptyLabel = new JLabel("");
    JTextArea foodA = new JTextArea("379");
    JTextArea animA = new JTextArea("390");

    //panels
    JTabbedPane tpanel = new JTabbedPane();
    JPanel mainPanel = new JPanel();
    JPanel lootPanel = new JPanel();
    JPanel combatPanel = new JPanel();


    //buttons
    JButton startButton = new JButton("Start");

    String[] tins = {"None", "Ancient Tab"};
    String[] touts = {"None", "Falador Tab", "Varrock Tab"};
    String[] locs = {"Tavelry Dungeon", "Edgeville Dungeon"};
    String[] food = {"None", "Trout", "Lobster"};
    JComboBox teleportIn = new JComboBox(tins);
    JComboBox teleportOut = new JComboBox(touts);
    JComboBox location = new JComboBox(locs);
    JComboBox foodBox = new JComboBox(food);

    //check boxes
    JCheckBox looting = new JCheckBox("looting");
    JCheckBox guamBox = new JCheckBox("guam leaf");
    JCheckBox marrentillBox = new JCheckBox("marrentill");
    JCheckBox tarrominBox = new JCheckBox("tarromin");
    JCheckBox harralanderBox = new JCheckBox("harralander");
    JCheckBox avantoeBox = new JCheckBox("avantoe");
    JCheckBox iritBox = new JCheckBox("irit leaf");
    JCheckBox kwuarmBox = new JCheckBox("kwuarm");
    JCheckBox ranarrBox = new JCheckBox("ranarr weed");
    JCheckBox lantadymeBox = new JCheckBox("lantadyme");
    JCheckBox dwarfBox = new JCheckBox("dwarf weed");
    JCheckBox cadantineBox = new JCheckBox("cadantine");

    JCheckBox airBox = new JCheckBox("air runes");
    JCheckBox earthBox = new JCheckBox("earth runes");
    JCheckBox bodyBox = new JCheckBox("body runes");
    JCheckBox mindBox = new JCheckBox("mind runes");
    JCheckBox lawBox = new JCheckBox("law runes");
    JCheckBox natureBox = new JCheckBox("nature runes");
    JCheckBox boltBox = new JCheckBox("mithril bolts");
    JCheckBox rareBox = new JCheckBox("rare drop table");

    JSlider foodAMT = new JSlider();


    void setup() {
        setSize(300,400);
        GridLayout gl = new GridLayout();
        gl.setRows(10);
        gl.setColumns(2);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        // main panel setup
        mainPanel.setLayout(gl);
        mainPanel.add(titleLabel);
        mainPanel.add(locationLabel);
        mainPanel.add(location);
        mainPanel.add(tinLabel);
        mainPanel.add(teleportIn);
        mainPanel.add(toutLabel);
        mainPanel.add(teleportOut);
        mainPanel.add(emptyLabel);
        mainPanel.add(emptyLabel);
        mainPanel.add(emptyLabel);
        mainPanel.add(startButton);

        // combat panel setup
        combatPanel.setLayout(gl);
        combatPanel.add(animationLabel);
        combatPanel.add(animA);
        combatPanel.add(foodLabel);
        combatPanel.add(foodBox);
        foodAMT.setMinimum(0);
        foodAMT.setMaximum(28);
        foodAMT.setMajorTickSpacing(1);
        foodAMT.setPaintTicks(true);
        foodAMT.setSnapToTicks(true);
        combatPanel.add(amountLabel);
        combatPanel.add(foodAMT);

        // loot panel setup
        lootPanel.setLayout(gl);
        lootPanel.add(guamBox);
        lootPanel.add(rareBox);
        lootPanel.add(marrentillBox);
        lootPanel.add(earthBox);
        lootPanel.add(tarrominBox);
        lootPanel.add(bodyBox);
        lootPanel.add(harralanderBox);
        lootPanel.add(mindBox);
        lootPanel.add(avantoeBox);
        lootPanel.add(lawBox);
        lootPanel.add(iritBox);
        lootPanel.add(natureBox);
        lootPanel.add(kwuarmBox);
        lootPanel.add(boltBox);
        lootPanel.add(ranarrBox);
        lootPanel.add(airBox);
        lootPanel.add(lantadymeBox);
        lootPanel.add(dwarfBox);
        lootPanel.add(cadantineBox);


        tpanel.addTab("Start", mainPanel);
        tpanel.addTab("Combat Settings", combatPanel);
        tpanel.addTab("Loot Settings", lootPanel);
        add(tpanel);

        startButton.addActionListener(this);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            started = true;
            setVisible(false);
        }
    }
}

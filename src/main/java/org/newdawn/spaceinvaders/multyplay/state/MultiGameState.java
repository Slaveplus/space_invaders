package org.newdawn.spaceinvaders.multyplay.state;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 멀티 게임 전체 상태 (싱글 GameStateManager 유사) - 이후 싱글 로직 포크 예정
 */
public class MultiGameState {
    public static final int MAX_PLAYERS = 4;

    private final Map<Integer, PlayerRuntimeState> players = new ConcurrentHashMap<>();
    // 라운드/보스/점수/메시지
    private int round = 1;
    private int maxRound = 6;
    private boolean bossRound = false;
    private boolean bossAlive = false;
    private int totalScore = 0;
    private String message = "";
    private boolean waitingForKeyPress = false;

    // 스킬 비용/성장 (간단 포크 값)
    private int attackPower = 1;
    private double attackSpeed = 1.0;
    private int attackPowerCost = 1;
    private int attackSpeedCost = 2;
    private int hpUpCost = 3;
    private int maxHP = 5;
    private int currentHP = maxHP;

    public synchronized void newGame(){
        round = 1; bossRound = false; bossAlive = false; totalScore = 0; message = ""; waitingForKeyPress=false;
        attackPower=1; attackSpeed=1.0; maxHP=5; currentHP=maxHP;
        players.values().forEach(p -> { p.skillPoints = 0; p.hp = maxHP; });
    }

    public synchronized void onAlienKilled(){
        addScore(10);
        players.values().forEach(p -> p.skillPoints += 1); // 모두에게 1포인트 배분
    }

    public synchronized void onBossSpawn(){ bossRound = true; bossAlive = true; message = "BOSS APPEARED"; waitingForKeyPress = true; }
    public synchronized void onBossDefeated(){ bossAlive = false; message = "BOSS DOWN"; waitingForKeyPress = true; addScore(500); }

    public synchronized boolean tryAdvanceRound(){
        if(round >= maxRound){ message = "GAME COMPLETE"; waitingForKeyPress=true; return false; }
        round++;
        bossRound = (round % 2 == 0); // 2,4,6 ...
        waitingForKeyPress = false; message = "";
        return true;
    }

    public synchronized void addScore(int s){ totalScore += s; }

    public synchronized boolean spendForAttackPower(){ if(getLeadPlayerSkillPoints() >= attackPowerCost){ adjustAllPlayers(p -> p.skillPoints -= attackPowerCost); attackPower++; attackPowerCost+=1; return true;} return false; }
    public synchronized boolean spendForAttackSpeed(){ if(getLeadPlayerSkillPoints() >= attackSpeedCost){ adjustAllPlayers(p -> p.skillPoints -= attackSpeedCost); attackSpeed+=0.2; attackSpeedCost+=1; return true;} return false; }
    public synchronized boolean spendForHpUp(){ if(getLeadPlayerSkillPoints() >= hpUpCost){ adjustAllPlayers(p -> p.skillPoints -= hpUpCost); maxHP+=3; currentHP=maxHP; players.values().forEach(p -> p.hp = maxHP); hpUpCost+=2; return true;} return false; }

    private int getLeadPlayerSkillPoints(){ return players.values().stream().mapToInt(p -> p.skillPoints).max().orElse(0); }
    private void adjustAllPlayers(Consumer<PlayerRuntimeState> c){ players.values().forEach(c); }

    public PlayerRuntimeState ensurePlayer(int slot) {
        return players.computeIfAbsent(slot, PlayerRuntimeState::new);
    }

    public Map<Integer, PlayerRuntimeState> getPlayers() { return players; }
    public int getRound() { return round; }
    public int getMaxRound(){ return maxRound; }
    public boolean isBossRound(){ return bossRound; }
    public boolean isBossAlive(){ return bossAlive; }
    public int getAttackPower(){ return attackPower; }
    public double getAttackSpeed(){ return attackSpeed; }
    public int getMaxHP(){ return maxHP; }
    public int getCurrentHP(){ return currentHP; }
    public String getMessage(){ return message; }
    public boolean isWaitingForKeyPress(){ return waitingForKeyPress; }
    public int getTotalScore(){ return totalScore; }
}

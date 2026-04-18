package org.example;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

public class VotingServer {
    private BigInteger n; 
    private BigInteger d; 
    private BigInteger e; 
    private Set<BigInteger> usedBlindedHashes; 
    private Set<String> usedVoters; 
    private Map<String, Integer> results; 
    private SecureRandom random;
    private int voteCounter; 

    public static final String[] VOTE_OPTIONS = {"ДА", "НЕТ", "ВОЗДЕРЖАЛСЯ"};
    public static final Map<String, BigInteger> VOTE_VALUES = new HashMap<>();

    static {
        VOTE_VALUES.put("ДА", BigInteger.valueOf(100));
        VOTE_VALUES.put("НЕТ", BigInteger.valueOf(200));
        VOTE_VALUES.put("ВОЗДЕРЖАЛСЯ", BigInteger.valueOf(300));
    }

    public VotingServer() {
        this.random = new SecureRandom();
        this.usedBlindedHashes = new HashSet<>();
        this.usedVoters = new HashSet<>();
        this.results = new HashMap<>();
        this.voteCounter = 1;

        for (String option : VOTE_OPTIONS) {
            results.put(option, 0);
        }

        generateRSAKeys();
        System.out.println("Сервер голосования инициализирован");
        System.out.println("Открытый ключ (e, n): (" + e + ", " + n + ")");
    }

    private void generateRSAKeys() {
        BigInteger p = BigInteger.probablePrime(256, random);
        BigInteger q = BigInteger.probablePrime(256, random);

        n = p.multiply(q);
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));

        e = BigInteger.valueOf(65537);
        while (!e.gcd(phi).equals(BigInteger.ONE)) {
            e = e.add(BigInteger.ONE);
        }

        d = e.modInverse(phi);
    }


    public boolean canVote(String voterId) {
        boolean canVote = !usedVoters.contains(voterId);
        System.out.println("Проверка избирателя " + voterId + ": " + (canVote ? "может голосовать" : "уже голосовал"));
        return canVote;
    }

    public BigInteger blindSign(BigInteger blindedHash, String voterId) {
        System.out.println("Сервер: Получен слепой бюллетень от " + voterId + ": " + blindedHash);

        if (usedVoters.contains(voterId)) {
            throw new RuntimeException("Избиратель " + voterId + " уже проголосовал!");
        }

        if (usedBlindedHashes.contains(blindedHash)) {
            throw new RuntimeException("Этот слепой хеш уже использовался!");
        }

        usedBlindedHashes.add(blindedHash);

        BigInteger signature = blindedHash.modPow(d, n);

        System.out.println("Сервер: Подписал слепой хеш для " + voterId);
        System.out.println("Сервер: Подпись: " + signature);

        return signature;
    }

    public boolean submitVote(BigInteger vote, BigInteger signature, String voterId, String voteOption) {
        System.out.println("Сервер: Получен голос от " + voterId + ": " + vote + " с подписью: " + signature);

        if (usedVoters.contains(voterId)) {
            System.out.println("Сервер: Избиратель " + voterId + " уже проголосовал!");
            return false;
        }

        BigInteger verified = signature.modPow(e, n);

        System.out.println("Сервер: Проверка подписи: " + verified + " == " + vote);

        if (!vote.equals(verified)) {
            System.out.println("Сервер: Неверная подпись!");
            return false;
        }

        if (!isValidVote(vote, voteOption)) {
            System.out.println("Сервер: Неверный вариант голоса: " + vote + " для " + voteOption);
            return false;
        }

        usedVoters.add(voterId);
        results.put(voteOption, results.get(voteOption) + 1);
        voteCounter++;

        System.out.println("Сервер: Голос принят: " + voteOption + " от " + voterId);
        System.out.println("Сервер: Текущие результаты: " + results);
        System.out.println("Сервер: Проголосовавшие: " + usedVoters);

        return true;
    }

    private boolean isValidVote(BigInteger vote, String voteOption) {
        BigInteger baseValue = VOTE_VALUES.get(voteOption);
        return vote.toString().startsWith(baseValue.toString());
    }

    public Map<String, Integer> getResults() {
        return new HashMap<>(results);
    }

    public Set<String> getVotedVoters() {
        return new HashSet<>(usedVoters);
    }

    public BigInteger getPublicKeyE() { return e; }
    public BigInteger getPublicKeyN() { return n; }
    public int getVoteCounter() { return voteCounter; }
}




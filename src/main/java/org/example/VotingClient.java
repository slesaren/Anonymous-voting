package org.example;

import java.math.BigInteger;
import java.security.SecureRandom;

public class VotingClient {
    private String voterId;
    private SecureRandom random;
    private VotingServer server;

    public VotingClient(String voterId, VotingServer server) {
        this.voterId = voterId;
        this.random = new SecureRandom();
        this.server = server;
        System.out.println("Избиратель " + voterId + " инициализирован");
    }

    public boolean canVote() {
        return server.canVote(voterId);
    }

    public BlindVote createBlindVote(String voteOption) {
        if (!server.canVote(voterId)) {
            throw new IllegalStateException("Избиратель " + voterId + " уже проголосовал!");
        }

        if (!VotingServer.VOTE_VALUES.containsKey(voteOption)) {
            throw new IllegalArgumentException("Неверный вариант голоса: " + voteOption);
        }

        BigInteger baseValue = VotingServer.VOTE_VALUES.get(voteOption);
        BigInteger uniqueId = BigInteger.valueOf(server.getVoteCounter());
        BigInteger vote = baseValue.multiply(BigInteger.valueOf(1000)).add(uniqueId);

        System.out.println("\n=== Избиратель " + voterId + " создает бюллетень ===");
        System.out.println("Выбранный вариант: " + voteOption);
        System.out.println("Базовое значение: " + baseValue);
        System.out.println("Уникальный идентификатор: " + uniqueId);
        System.out.println("Итоговое значение голоса: " + vote);

        BigInteger n = server.getPublicKeyN();
        BigInteger r;
        do {
            r = new BigInteger(n.bitLength() - 1, random);
        } while (!r.gcd(n).equals(BigInteger.ONE));

        BigInteger rInverse = r.modInverse(n);

        System.out.println("Слепящий множитель r: " + r);
        System.out.println("Обратный множитель r^(-1): " + rInverse);

        BigInteger e = server.getPublicKeyE();
        BigInteger rPowE = r.modPow(e, n);
        BigInteger blindedVote = vote.multiply(rPowE).mod(n);

        System.out.println("r^e mod n: " + rPowE);
        System.out.println("Слепой бюллетень: " + blindedVote);

        return new BlindVote(vote, r, rInverse, blindedVote, voteOption);
    }


    public BigInteger unblindSignature(BigInteger blindedSignature, BlindVote blindVote) {
        System.out.println("\n=== Избиратель " + voterId + " ослепляет подпись ===");
        System.out.println("Слепая подпись от сервера: " + blindedSignature);

        BigInteger signature = blindedSignature.multiply(blindVote.getRInverse())
                .mod(server.getPublicKeyN());

        System.out.println("Итоговая подпись: " + signature);

        BigInteger verified = signature.modPow(server.getPublicKeyE(), server.getPublicKeyN());
        System.out.println("Проверка подписи: " + verified.equals(blindVote.getOriginalVote()) +
                " (ожидалось: " + blindVote.getOriginalVote() + ", получено: " + verified + ")");

        return signature;
    }

    public boolean submitVote(BigInteger vote, BigInteger signature, String voteOption) {
        System.out.println("\n=== Избиратель " + voterId + " отправляет голос ===");
        System.out.println("Голос: " + vote);
        System.out.println("Подпись: " + signature);
        System.out.println("Вариант: " + voteOption);

        return server.submitVote(vote, signature, voterId, voteOption);
    }

    public String getVoterId() {
        return voterId;
    }

  
    public static class BlindVote {
        private final BigInteger originalVote;
        private final BigInteger r;
        private final BigInteger rInverse;
        private final BigInteger blindedVote;
        private final String voteOption;

        public BlindVote(BigInteger originalVote, BigInteger r, BigInteger rInverse,
                         BigInteger blindedVote, String voteOption) {
            this.originalVote = originalVote;
            this.r = r;
            this.rInverse = rInverse;
            this.blindedVote = blindedVote;
            this.voteOption = voteOption;
        }

        public BigInteger getOriginalVote() { return originalVote; }
        public BigInteger getR() { return r; }
        public BigInteger getRInverse() { return rInverse; }
        public BigInteger getBlindedVote() { return blindedVote; }
        public String getVoteOption() { return voteOption; }
    }
}

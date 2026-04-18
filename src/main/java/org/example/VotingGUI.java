package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Map;

public class VotingGUI extends JFrame {
    private VotingServer server;
    private VotingClient currentClient;
    private JTextArea logArea;
    private JComboBox<String> voterSelector;
    private JComboBox<String> voteSelector;
    private JButton createVoteButton;
    private JButton signButton;
    private JButton submitButton;
    private JButton resultsButton;
    private JButton resetButton;

    private VotingClient.BlindVote currentBlindVote;
    private BigInteger currentSignature;
    private String currentVoter;

    public VotingGUI() {
        this.server = new VotingServer();
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("Система анонимного голосования со слепой подписью");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel(new GridLayout(2, 1));

        JPanel voterPanel = new JPanel();
        voterPanel.add(new JLabel("Избиратель:"));
        String[] voters = {"Иванов", "Петров", "Сидоров", "Козлов"};
        voterSelector = new JComboBox<>(voters);
        voterPanel.add(voterSelector);

        JPanel votePanel = new JPanel();
        votePanel.add(new JLabel("Голос:"));
        voteSelector = new JComboBox<>(VotingServer.VOTE_OPTIONS);
        votePanel.add(voteSelector);

        controlPanel.add(voterPanel);
        controlPanel.add(votePanel);

        JPanel buttonPanel = new JPanel();
        createVoteButton = new JButton("1. Создать бюллетень");
        signButton = new JButton("2. Получить слепую подпись");
        submitButton = new JButton("3. Отправить голос");
        resultsButton = new JButton("Показать результаты");
        resetButton = new JButton("Сброс для нового избирателя");

        createVoteButton.addActionListener(e -> createBlindVote());
        signButton.addActionListener(e -> getBlindSignature());
        submitButton.addActionListener(e -> submitVote());
        resultsButton.addActionListener(e -> showResults());
        resetButton.addActionListener(e -> resetForNewVoter());

        buttonPanel.add(createVoteButton);
        buttonPanel.add(signButton);
        buttonPanel.add(submitButton);
        buttonPanel.add(resultsButton);
        buttonPanel.add(resetButton);

        logArea = new JTextArea(20, 60);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        add(controlPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        resetForNewVoter(); 

        pack();
        setLocationRelativeTo(null);

        log("=== СИСТЕМА АНОНИМНОГО ГОЛОСОВАНИЯ ===");
        log("Сервер инициализирован с открытым ключом RSA");
        log("Модуль n: " + server.getPublicKeyN());
        log("Открытая экспонента e: " + server.getPublicKeyE());
        log("\nИнструкция:");
        log("1. Выберите избирателя и вариант голоса");
        log("2. Нажмите 'Создать бюллетень'");
        log("3. Нажмите 'Получить слепую подпись'");
        log("4. Нажмите 'Отправить голос'");
        log("5. Для нового избирателя нажмите 'Сброс для нового избирателя'");
    }

    private void createBlindVote() {
        try {
            String voterName = (String) voterSelector.getSelectedItem();
            String voteOption = (String) voteSelector.getSelectedItem();

            currentVoter = voterName;
            currentClient = new VotingClient(voterName, server);
            currentBlindVote = currentClient.createBlindVote(voteOption);
            currentSignature = null; // Сбрасываем предыдущую подпись

            log("\n=== БЮЛЛЕТЕНЬ СОЗДАН ===");
            log("Избиратель: " + voterName);
            log("Вариант голоса: " + voteOption);
            log("Числовое значение: " + currentBlindVote.getOriginalVote());
            log("Слепящий множитель r: " + currentBlindVote.getR());
            log("Обратный множитель r⁻¹: " + currentBlindVote.getRInverse());
            log("Слепой бюллетень: " + currentBlindVote.getBlindedVote());
            log("Теперь нажмите 'Получить слепую подпись'");

            signButton.setEnabled(true);
            submitButton.setEnabled(false);

        } catch (Exception e) {
            log("Ошибка при создании бюллетеня: " + e.getMessage());
        }
    }

    private void getBlindSignature() {
        try {
            if (currentBlindVote == null) {
                log("Сначала создайте бюллетень!");
                return;
            }

            log("\n=== ПОЛУЧЕНИЕ СЛЕПОЙ ПОДПИСИ ===");
            log("Отправляем слепой бюллетень на сервер: " + currentBlindVote.getBlindedVote());

            BigInteger blindedSignature = server.blindSign(currentBlindVote.getBlindedVote(), currentVoter);

            log("Получена слепая подпись от сервера: " + blindedSignature);

            currentSignature = currentClient.unblindSignature(blindedSignature, currentBlindVote);

            log("Ослепленная подпись: " + currentSignature);
            log("Теперь нажмите 'Отправить голос'");

            submitButton.setEnabled(true);
            signButton.setEnabled(false);

        } catch (Exception e) {
            log("Ошибка при получении подписи: " + e.getMessage());
        }
    }

    private void submitVote() {
        try {
            if (currentBlindVote == null || currentSignature == null) {
                log("Сначала получите подпись!");
                return;
            }

            log("\n=== ОТПРАВКА ГОЛОСА ===");
            log("Отправляем голос: " + currentBlindVote.getOriginalVote());
            log("С подписью: " + currentSignature);
            log("Вариант: " + currentBlindVote.getVoteOption());

            boolean success = currentClient.submitVote(
                    currentBlindVote.getOriginalVote(),
                    currentSignature,
                    currentBlindVote.getVoteOption()
            );

            if (success) {
                log("Голос успешно принят!");
                log("Избиратель " + currentVoter + " завершил голосование.");


                showResults();
            } else {
                log("Ошибка при принятии голоса!");
                log("Возможно, этот избиратель уже голосовал с таким значением.");
            }

            createVoteButton.setEnabled(false);
            signButton.setEnabled(false);
            submitButton.setEnabled(false);
            resetButton.setEnabled(true);
            resultsButton.setEnabled(true);

        } catch (Exception e) {
            log("Ошибка при отправке голоса: " + e.getMessage());
        }
    }

    private void resetForNewVoter() {
        currentBlindVote = null;
        currentSignature = null;
        currentClient = null;
        currentVoter = null;

        voterSelector.setSelectedIndex(0);
        voteSelector.setSelectedIndex(0);

        createVoteButton.setEnabled(true);
        signButton.setEnabled(false);
        submitButton.setEnabled(false);
        resetButton.setEnabled(true);
        resultsButton.setEnabled(true);

        log("\n=== СБРОС ===");
        log("Система готова для нового избирателя");
        log("Выберите избирателя и вариант голоса");
    }

    private void showResults() {
        Map<String, Integer> results = server.getResults();

        log("\n=== РЕЗУЛЬТАТЫ ГОЛОСОВАНИЯ ===");
        for (Map.Entry<String, Integer> entry : results.entrySet()) {
            log(entry.getKey() + ": " + entry.getValue() + " голосов");
        }

        int total = results.values().stream().mapToInt(Integer::intValue).sum();
        log("Всего голосов: " + total);
        log("Всего избирателей: " + total);
    }

    private void log(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.err.println("Ошибка настройки кодировки вывода: " + e.getMessage());
        }
        SwingUtilities.invokeLater(() -> {
            VotingGUI gui = new VotingGUI();
            gui.setVisible(true);
        });
    }
}
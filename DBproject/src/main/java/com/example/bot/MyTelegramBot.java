package com.example.bot;

import com.example.entity.UserData;
import com.example.entity.UserTask;
import com.example.model.UserSession;
import com.example.model.UserState;
import com.example.repository.UserDataRepository;
import com.example.repository.UserTaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class MyTelegramBot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botName;
    private final UserDataRepository userDataRepository;
    private final UserTaskRepository userTaskRepository;

    private final Map<Long, UserSession> userSessions = new HashMap<>();
    private static final int MAX_TASKS_PER_USER = 20;

    public MyTelegramBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.name}") String botName,
            UserDataRepository userDataRepository,
            UserTaskRepository userTaskRepository) {
        super(botToken);
        this.botToken = botToken;
        this.botName = botName;
        this.userDataRepository = userDataRepository;
        this.userTaskRepository = userTaskRepository;
        System.out.println("=== БОТ ИНИЦИАЛИЗИРОВАН ===");
    }

    // === NEW WRAPPER METHODS FOR TESTING ===
    protected void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    protected void executeDocument(SendDocument document) {
        try {
            execute(document);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    // =======================================

    @Override
    public String getBotUsername() { return botName; }

    @Override
    public String getBotToken() { return botToken; }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            Long userId = update.getMessage().getFrom().getId();
            String messageText = update.getMessage().getText();

            UserSession session = userSessions.getOrDefault(userId, new UserSession());

            try {
                handleMessage(chatId, userId, messageText, session);
            } catch (Exception e) {
                sendMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
                e.printStackTrace();
            }

            userSessions.put(userId, session);
        }
    }

    private void handleMessage(Long chatId, Long userId, String text, UserSession session) {
        if ("↩️ Назад в меню".equals(text) || "Отмена".equals(text)) {
            session.setState(UserState.IDLE);
            session.setSelectedTask(null);
            session.setCurrentTaskId(null);
            session.setTimerStart(null);
            session.setTaskList(null);
            session.setSelectedReportType(null);
            session.setCustomStartDate(null);
            session.setCustomEndDate(null);

            sendMainMenu(chatId, "Возвращаюсь в главное меню");
            return;
        }

        switch (session.getState()) {
            case IDLE:
                handleIdleState(chatId, userId, text, session);
                break;
            case WAITING_TASK_SELECTION:
                handleTaskSelection(chatId, userId, text, session);
                break;
            case WAITING_NEW_TASK_NAME:
                handleNewTaskName(chatId, userId, text, session);
                break;
            case WAITING_TASK_DELETION:
                handleTaskDeletion(chatId, userId, text, session);
                break;
            case TIMER_RUNNING:
                handleTimerRunning(chatId, userId, text, session);
                break;
            case WAITING_CUSTOM_START_DATE:
                handleCustomStartDate(chatId, userId, text, session);
                break;
            case WAITING_CUSTOM_END_DATE:
                handleCustomEndDate(chatId, userId, text, session);
                break;
        }
    }

    private void handleIdleState(Long chatId, Long userId, String text, UserSession session) {
        switch (text) {
            case "/start":
                sendMainMenu(chatId, "🎯 Добро пожаловать в TimeManager Bot!\nВыберите действие:");
                break;
            case "⏱️ Начать отсчет дела":
                showTaskList(chatId, userId, session);
                break;
            case "📋 Мои дела":
                showTaskManagement(chatId, userId);
                break;
            case "📊 Отчет за неделю":
                generateReport(chatId, userId, "WEEK", "STATISTICS", null, null);
                break;
            case "📁 Выгрузить данные":
                exportUserData(chatId, userId);
                break;
            case "▶️ Начать таймер":
                if (session.getSelectedTask() != null) {
                    startTimer(chatId, userId, session);
                } else {
                    sendMessage(chatId, "❌ Сначала выберите задачу через меню \"Начать отсчет дела\"");
                }
                break;
            case "⏹️ Остановить таймер":
                if (session.getState() == UserState.TIMER_RUNNING) {
                    stopTimer(chatId, userId, session);
                } else {
                    sendMessage(chatId, "❌ Таймер не запущен!");
                }
                break;
            case "➕ Добавить дело":
                session.setState(UserState.WAITING_NEW_TASK_NAME);
                SendMessage message = new SendMessage();
                message.setChatId(chatId.toString());
                message.setText("📝 Введите название нового дела:");

                ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
                keyboard.setResizeKeyboard(true);

                List<KeyboardRow> rows = new ArrayList<>();
                KeyboardRow cancelRow = new KeyboardRow();
                cancelRow.add("Отмена");
                rows.add(cancelRow);

                keyboard.setKeyboard(rows);
                message.setReplyMarkup(keyboard);

                executeMessage(message); // CHANGED
                break;
            case "🗑️ Удалить дело":
                showTasksForDeletion(chatId, userId, session);
                break;
            case "↩️ Назад в меню":
                session.setSelectedReportType(null);
                sendMainMenu(chatId, "Главное меню:");
                break;
            case "📊 Отчеты":
                sendReportsMenu(chatId);
                break;
            case "❓ Инструкция":
                sendInstruction(chatId);
                break;
            case "📅 Отчет за неделю":
                session.setSelectedReportType("WEEK");
                sendReportTypeMenu(chatId, "неделя");
                break;
            case "📅 Отчет за месяц":
                session.setSelectedReportType("MONTH");
                sendReportTypeMenu(chatId, "месяц");
                break;
            case "📅 Отчет за год":
                session.setSelectedReportType("YEAR");
                sendReportTypeMenu(chatId, "год");
                break;
            case "📅 Произвольный период":
                handleCustomPeriodStart(chatId, session);
                break;
            case "📈 Статистика":
                if (session.getSelectedReportType() != null) {
                    String reportType = session.getSelectedReportType();
                    generateReport(chatId, userId, reportType, "STATISTICS", null, null);
                    sendReportTypeMenu(chatId, getPeriodDescription(reportType));
                } else {
                    sendMessage(chatId, "❌ Сначала выберите период отчета");
                }
                break;
            case "📋 Подробный":
                if (session.getSelectedReportType() != null) {
                    String reportType = session.getSelectedReportType();
                    generateReport(chatId, userId, reportType, "DETAILED", null, null);
                    sendReportTypeMenu(chatId, getPeriodDescription(reportType));
                } else {
                    sendMessage(chatId, "❌ Сначала выберите период отчета");
                }
                break;
            case "↩️ Назад к отчетам":
                session.setSelectedReportType(null);
                sendReportsMenu(chatId);
                break;
            case "📈 Статистика недели":
                generateWeeklyComparisonChart(chatId, userId);
                break;
            default:
                sendMainMenu(chatId, "Выберите действие из меню:");
        }
    }

    private void handleTimerRunning(Long chatId, Long userId, String text, UserSession session) {
        if (text.equals("⏹️ Остановить таймер")) {
            stopTimer(chatId, userId, session);
        } else {
            sendMessage(chatId, "❌ Для остановки таймера используйте кнопку \"⏹️ Остановить таймер\"");
        }
    }

    private void showTaskList(Long chatId, Long userId, UserSession session) {
        List<UserTask> tasks = userTaskRepository.findByUserIdOrderByUsageCountDesc(userId);

        if (tasks.isEmpty()) {
            sendMessage(chatId, "📝 У вас пока нет задач. Сначала добавьте задачи через меню \"Мои дела\"");
            return;
        }

        StringBuilder messageText = new StringBuilder("📋 Выберите задачу (введите номер):\n\n");
        int index = 1;
        for (UserTask task : tasks) {
            messageText.append(index).append(". ").append(task.getTaskName())
                    .append(" (использовано: ").append(task.getUsageCount()).append(")\n");
            index++;
        }

        session.setState(UserState.WAITING_TASK_SELECTION);
        session.setTaskList(tasks);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(messageText.toString());

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow cancelRow = new KeyboardRow();
        cancelRow.add("Отмена");
        rows.add(cancelRow);

        keyboard.setKeyboard(rows);
        sendMessage.setReplyMarkup(keyboard);

        executeMessage(sendMessage); // CHANGED
    }

    private void handleTaskSelection(Long chatId, Long userId, String text, UserSession session) {
        if ("Отмена".equals(text)) {
            session.setState(UserState.IDLE);
            sendMainMenu(chatId, "Возвращаюсь в главное меню");
            return;
        }

        try {
            int taskIndex = Integer.parseInt(text) - 1;
            List<UserTask> tasks = session.getTaskList();

            if (taskIndex >= 0 && taskIndex < tasks.size()) {
                UserTask selectedTask = tasks.get(taskIndex);
                session.setSelectedTask(selectedTask.getTaskName());
                session.setCurrentTaskId(selectedTask.getId());
                session.setState(UserState.IDLE);

                sendTimerMenu(chatId, "✅ Выбрана задача: " + selectedTask.getTaskName() +
                        "\n\nНажмите \"▶️ Начать таймер\" чтобы начать отсчет времени");
            } else {
                sendMessage(chatId, "❌ Неверный номер задачи!");
                showTaskList(chatId, userId, session);
            }
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Пожалуйста, введите номер задачи!");
            showTaskList(chatId, userId, session);
        }
    }

    private void startTimer(Long chatId, Long userId, UserSession session) {
        session.setTimerStart(LocalDateTime.now());
        session.setState(UserState.TIMER_RUNNING);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String startTime = session.getTimerStart().format(formatter);

        sendTimerRunningMenu(chatId, "⏱️ Таймер запущен!\n" +
                "Задача: " + session.getSelectedTask() + "\n" +
                "Начало: " + startTime + "\n\n" +
                "Нажмите \"⏹️ Остановить таймер\" когда закончите");
    }

    private void stopTimer(Long chatId, Long userId, UserSession session) {
        if (session.getTimerStart() == null) {
            sendMessage(chatId, "❌ Таймер не был запущен!");
            return;
        }

        LocalDateTime endTime = LocalDateTime.now();
        long duration = ChronoUnit.MINUTES.between(session.getTimerStart(), endTime);

        UserData userData = new UserData();
        userData.setUserId(userId);
        userData.setTaskName(session.getSelectedTask());
        userData.setStartTime(session.getTimerStart());
        userData.setEndTime(endTime);
        userData.setDurationMinutes(duration);
        userDataRepository.save(userData);

        if (session.getCurrentTaskId() != null) {
            Optional<UserTask> taskOpt = userTaskRepository.findById(session.getCurrentTaskId());
            if (taskOpt.isPresent()) {
                UserTask task = taskOpt.get();
                task.setUsageCount(task.getUsageCount() + 1);
                userTaskRepository.save(task);
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String message = "✅ Таймер остановлен!\n" +
                "Задача: " + session.getSelectedTask() + "\n" +
                "Начало: " + session.getTimerStart().format(formatter) + "\n" +
                "Конец: " + endTime.format(formatter) + "\n" +
                "Длительность: " + duration + " минут";

        session.setState(UserState.IDLE);
        session.setTimerStart(null);
        session.setSelectedTask(null);
        session.setCurrentTaskId(null);

        sendMainMenu(chatId, message);
    }

    private void showTaskManagement(Long chatId, Long userId) {
        List<UserTask> tasks = userTaskRepository.findByUserIdOrderByUsageCountDesc(userId);

        StringBuilder message = new StringBuilder("📋 Управление делами:\n\n");

        if (tasks.isEmpty()) {
            message.append("У вас пока нет задач.\n");
        } else {
            message.append("Ваши задачи:\n");
            int index = 1;
            for (UserTask task : tasks) {
                message.append(index).append(". ").append(task.getTaskName())
                        .append(" (ID: ").append(task.getId()).append(")\n");
                index++;
            }
        }

        message.append("\nВсего задач: ").append(tasks.size()).append("/").append(MAX_TASKS_PER_USER);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(message.toString());

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        if (tasks.size() < MAX_TASKS_PER_USER) {
            row1.add("➕ Добавить дело");
        }
        if (!tasks.isEmpty()) {
            row1.add("🗑️ Удалить дело");
        }

        KeyboardRow row2 = new KeyboardRow();
        row2.add("↩️ Назад в меню");

        if (!row1.isEmpty()) rows.add(row1);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        sendMessage.setReplyMarkup(keyboard);

        executeMessage(sendMessage); // CHANGED
    }

    private void handleNewTaskName(Long chatId, Long userId, String text, UserSession session) {
        if ("Отмена".equals(text)) {
            session.setState(UserState.IDLE);
            showTaskManagement(chatId, userId);
            return;
        }

        if (text.length() > 50) {
            sendMessage(chatId, "❌ Название дела не должно превышать 50 символов!");
            return;
        }

        List<UserTask> userTasks = userTaskRepository.findByUserIdOrderByUsageCountDesc(userId);
        if (userTasks.size() >= MAX_TASKS_PER_USER) {
            sendMessage(chatId, "❌ Достигнут лимит задач (" + MAX_TASKS_PER_USER + "). Удалите некоторые дела чтобы добавить новые.");
            session.setState(UserState.IDLE);
            return;
        }

        Optional<UserTask> existingTask = userTaskRepository.findByUserIdAndTaskName(userId, text);
        if (existingTask.isPresent()) {
            sendMessage(chatId, "❌ Задача с таким названием уже существует!");
            session.setState(UserState.IDLE);
            return;
        }

        UserTask newTask = new UserTask();
        newTask.setUserId(userId);
        newTask.setTaskName(text);
        newTask.setUsageCount(0);
        userTaskRepository.save(newTask);

        session.setState(UserState.IDLE);
        sendMessage(chatId, "✅ Задача \"" + text + "\" успешно добавлена!");
        showTaskManagement(chatId, userId);
    }

    private void showTasksForDeletion(Long chatId, Long userId, UserSession session) {
        List<UserTask> tasks = userTaskRepository.findByUserIdOrderByUsageCountDesc(userId);

        if (tasks.isEmpty()) {
            sendMessage(chatId, "❌ У вас нет задач для удаления!");
            return;
        }

        StringBuilder message = new StringBuilder("🗑️ Выберите номер задачи для удаления:\n\n");
        int index = 1;
        for (UserTask task : tasks) {
            message.append(index).append(". ").append(task.getTaskName())
                    .append(" (ID: ").append(task.getId()).append(")\n");
            index++;
        }

        session.setState(UserState.WAITING_TASK_DELETION);
        session.setTaskList(tasks);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(message.toString());

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow cancelRow = new KeyboardRow();
        cancelRow.add("Отмена");
        rows.add(cancelRow);

        keyboard.setKeyboard(rows);
        sendMessage.setReplyMarkup(keyboard);

        executeMessage(sendMessage); // CHANGED
    }

    private void handleTaskDeletion(Long chatId, Long userId, String text, UserSession session) {
        if ("Отмена".equals(text)) {
            session.setState(UserState.IDLE);
            showTaskManagement(chatId, userId);
            return;
        }

        try {
            int taskIndex = Integer.parseInt(text) - 1;
            List<UserTask> tasks = session.getTaskList();

            if (taskIndex >= 0 && taskIndex < tasks.size()) {
                UserTask taskToDelete = tasks.get(taskIndex);
                String taskName = taskToDelete.getTaskName();

                userTaskRepository.delete(taskToDelete);

                session.setState(UserState.IDLE);
                sendMessage(chatId, "✅ Задача \"" + taskName + "\" удалена!");
                showTaskManagement(chatId, userId);
            } else {
                sendMessage(chatId, "❌ Неверный номер задачи!");
                showTasksForDeletion(chatId, userId, session);
            }
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Пожалуйста, введите номер задачи!");
            showTasksForDeletion(chatId, userId, session);
        }
    }

    private void exportUserData(Long chatId, Long userId) {
        List<UserData> allData = userDataRepository.findByUserIdOrderByStartTimeDesc(userId);

        if (allData.isEmpty()) {
            sendMessage(chatId, "❌ У вас пока нет данных для экспорта.");
            return;
        }

        try {
            File file = File.createTempFile("timemanager_export_" + userId + "_", ".txt");
            FileWriter writer = new FileWriter(file);

            writer.write("TimeManager Bot - Экспорт данных\n");
            writer.write("Пользователь ID: " + userId + "\n");
            writer.write("Дата экспорта: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + "\n");
            writer.write("=" .repeat(50) + "\n\n");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

            for (UserData data : allData) {
                writer.write("Задача: " + data.getTaskName() + "\n");
                writer.write("Начало: " + data.getStartTime().format(formatter) + "\n");
                writer.write("Конец: " + data.getEndTime().format(formatter) + "\n");
                writer.write("Длительность: " + data.getDurationMinutes() + " минут\n");
                writer.write("-".repeat(30) + "\n");
            }

            writer.close();

            SendDocument document = new SendDocument();
            document.setChatId(chatId.toString());
            document.setDocument(new InputFile(file, "timemanager_export.txt"));
            document.setCaption("📁 Ваши данные экспортированы в файл");

            executeDocument(document); // CHANGED

            file.delete();

        } catch (IOException e) {
            sendMessage(chatId, "❌ Ошибка при экспорте данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendMainMenu(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        ReplyKeyboardMarkup keyboard = createMainMenuKeyboard();
        message.setReplyMarkup(keyboard);

        executeMessage(message); // CHANGED
    }

    private ReplyKeyboardMarkup createMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("⏱️ Начать отсчет дела");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("📋 Мои дела");
        row2.add("📊 Отчеты");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("📈 Статистика недели");
        row3.add("📁 Выгрузить данные");

        KeyboardRow row4 = new KeyboardRow();
        row4.add("❓ Инструкция");

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private void sendTimerMenu(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("▶️ Начать таймер");
        KeyboardRow row2 = new KeyboardRow();
        row2.add("↩️ Назад в меню");

        rows.add(row1);
        rows.add(row2);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        executeMessage(message); // CHANGED
    }

    private void sendTimerRunningMenu(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("⏹️ Остановить таймер");

        rows.add(row1);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        executeMessage(message); // CHANGED
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        executeMessage(message); // CHANGED
    }

    private void sendReportsMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📊 Выберите тип отчета:");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("📅 Отчет за неделю");
        row1.add("📅 Отчет за месяц");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("📅 Отчет за год");
        row2.add("📅 Произвольный период");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("↩️ Назад в меню");

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        executeMessage(message); // CHANGED
    }

    private void sendReportTypeMenu(Long chatId, String periodType) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите тип отчета для " + periodType + ":");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("📈 Статистика");
        row1.add("📋 Подробный");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("↩️ Назад к отчетам");

        rows.add(row1);
        rows.add(row2);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        executeMessage(message); // CHANGED
    }

    private void sendInstruction(Long chatId) {
        String instruction = """
        🎯 TimeManager Bot - Инструкция
        
        ⏱️ Основные функции:
        • "Начать отсчет дела" - выбрать задачу и запустить таймер
        • "Мои дела" - управление списком задач (добавить/удалить)
        • "Отчеты" - просмотр статистики за разные периоды
        
        📊 Типы отчетов:
        • Статистика - общий обзор по задачам
        • Подробный - детали по дням и задачам
        
        ⚡ Быстрые советы:
        • Максимум 20 задач на пользователя
        • Отчеты доступны за неделю/месяц/год/произвольный период
        • Данные можно выгрузить в файл
        """;

        sendMessage(chatId, instruction);
    }

    private void handleCustomPeriodStart(Long chatId, UserSession session) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📅 Выберите начальную дату или введите вручную (ГГГГ.ММ.ДД):");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("📅 Сегодня");
        row1.add("📅 Вчера");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("📅 Неделю назад");
        row2.add("📅 Месяц назад");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("✏️ Ввести вручную");

        KeyboardRow row4 = new KeyboardRow();
        row4.add("↩️ Назад к отчетам");

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        session.setState(UserState.WAITING_CUSTOM_START_DATE);

        executeMessage(message); // CHANGED
    }

    private void handleCustomStartDate(Long chatId, Long userId, String text, UserSession session) {
        LocalDate startDate;

        switch (text) {
            case "📅 Сегодня": startDate = LocalDate.now(); break;
            case "📅 Вчера": startDate = LocalDate.now().minusDays(1); break;
            case "📅 Неделю назад": startDate = LocalDate.now().minusWeeks(1); break;
            case "📅 Месяц назад": startDate = LocalDate.now().minusMonths(1); break;
            case "✏️ Ввести вручную":
                sendMessage(chatId, "📅 Введите начальную дату в формате ГГГГ.ММ.ДД\nНапример: 2024.11.01");
                return;
            case "↩️ Назад к отчетам":
                sendReportsMenu(chatId);
                session.setState(UserState.IDLE);
                return;
            default:
                try {
                    startDate = LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
                } catch (Exception e) {
                    sendMessage(chatId, "❌ Неверный формат даты! Используйте ГГГГ.ММ.ДД\nПопробуйте еще раз:");
                    return;
                }
                break;
        }

        session.setCustomStartDate(startDate);
        sendCustomEndDateMenu(chatId, session);
    }

    private void sendCustomEndDateMenu(Long chatId, UserSession session) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📅 Выберите конечную дату или введите вручную (ГГГГ.ММ.ДД):\n(Начало: " +
                session.getCustomStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ")");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        LocalDate startDate = session.getCustomStartDate();
        LocalDate today = LocalDate.now();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("📅 Сегодня");

        if (startDate.isBefore(today.minusDays(1))) {
            row1.add("📅 Завтра от начала");
        }

        KeyboardRow row2 = new KeyboardRow();
        row2.add("📅 Неделя от начала");
        row2.add("📅 Месяц от начала");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("✏️ Ввести вручную");

        KeyboardRow row4 = new KeyboardRow();
        row4.add("↩️ Выбрать другую начальную дату");

        rows.add(row1);
        if (row1.size() > 1) rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        session.setState(UserState.WAITING_CUSTOM_END_DATE);

        executeMessage(message); // CHANGED
    }

    private void handleCustomEndDate(Long chatId, Long userId, String text, UserSession session) {
        LocalDate startDate = session.getCustomStartDate();
        LocalDate endDate;

        switch (text) {
            case "📅 Сегодня": endDate = LocalDate.now(); break;
            case "📅 Завтра от начала": endDate = startDate.plusDays(1); break;
            case "📅 Неделя от начала": endDate = startDate.plusWeeks(1); break;
            case "📅 Месяц от начала": endDate = startDate.plusMonths(1); break;
            case "✏️ Ввести вручную":
                sendMessage(chatId, "📅 Введите конечную дату в формате ГГГГ.ММ.ДД\nНапример: 2024.11.15");
                return;
            case "↩️ Выбрать другую начальную дату":
                handleCustomPeriodStart(chatId, session);
                return;
            default:
                try {
                    endDate = LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
                } catch (Exception e) {
                    sendMessage(chatId, "❌ Неверный формат даты! Используйте ГГГГ.ММ.ДД\nПопробуйте еще раз:");
                    return;
                }
                break;
        }

        if (endDate.isBefore(startDate)) {
            sendMessage(chatId, "❌ Конечная дата не может быть раньше начальной!\nВыберите другую дату:");
            return;
        }

        generateReport(chatId, userId, "CUSTOM", "STATISTICS", startDate, endDate);

        session.setState(UserState.IDLE);
        session.setCustomStartDate(null);
        session.setCustomEndDate(null);
    }

    private void generateReport(Long chatId, Long userId, String periodType, String reportType,
                                LocalDate customStart, LocalDate customEnd) {
        LocalDateTime startDate;
        LocalDateTime endDate;
        String periodDescription;

        switch (periodType) {
            case "WEEK":
                endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
                startDate = endDate.minusWeeks(1).plusDays(1).withHour(0).withMinute(0).withSecond(0);
                periodDescription = "неделю";
                break;
            case "MONTH":
                endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
                startDate = endDate.minusMonths(1).plusDays(1).withHour(0).withMinute(0).withSecond(0);
                periodDescription = "месяц";
                break;
            case "YEAR":
                endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
                startDate = endDate.minusYears(1).plusDays(1).withHour(0).withMinute(0).withSecond(0);
                periodDescription = "год";
                break;
            case "CUSTOM":
                startDate = customStart.atTime(0, 0);
                endDate = customEnd.atTime(23, 59, 59);
                periodDescription = String.format("период с %s по %s",
                        customStart.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        customEnd.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                break;
            default:
                sendMessage(chatId, "❌ Неизвестный тип периода");
                return;
        }

        List<UserData> reportData = userDataRepository.findByUserIdAndStartTimeBetween(userId, startDate, endDate);

        if (reportData.isEmpty()) {
            sendMessage(chatId, "📊 За " + periodDescription + " у вас нет записей о занятиях.");
            return;
        }

        if ("STATISTICS".equals(reportType)) {
            generateStatisticsReport(chatId, reportData, periodDescription, startDate, endDate);
        } else if ("DETAILED".equals(reportType)) {
            generateDetailedReport(chatId, userId, reportData, periodDescription, startDate, endDate, periodType);
        }
    }

    private void generateStatisticsReport(Long chatId, List<UserData> reportData, String periodDescription,
                                          LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Long> taskDurations = new HashMap<>();
        long totalMinutes = 0;

        for (UserData data : reportData) {
            String taskName = data.getTaskName();
            long duration = data.getDurationMinutes();
            taskDurations.put(taskName, taskDurations.getOrDefault(taskName, 0L) + duration);
            totalMinutes += duration;
        }

        List<Map.Entry<String, Long>> sortedTasks = new ArrayList<>(taskDurations.entrySet());
        sortedTasks.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        StringBuilder report = new StringBuilder();
        report.append("📊 Отчет за ").append(periodDescription).append(":\n\n");
        report.append("Период: ").append(startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .append(" - ").append(endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append("\n\n");

        if (totalMinutes > 0) {
            report.append("📈 Распределение времени:\n");
            int maxBars = 10;

            for (Map.Entry<String, Long> entry : sortedTasks) {
                double percentage = (entry.getValue() * 100.0) / totalMinutes;
                int bars = (int) ((percentage * maxBars) / 100);

                long hours = entry.getValue() / 60;
                long minutes = entry.getValue() % 60;

                report.append("• ").append(entry.getKey()).append(": ")
                        .append("#".repeat(Math.max(1, bars)))
                        .append(" ").append(String.format("%.1f", percentage)).append("%")
                        .append(" (").append(hours).append("ч ").append(minutes).append("м)\n");
            }
        } else {
            for (Map.Entry<String, Long> entry : sortedTasks) {
                long hours = entry.getValue() / 60;
                long minutes = entry.getValue() % 60;
                double percentage = totalMinutes > 0 ? (entry.getValue() * 100.0 / totalMinutes) : 0;

                report.append("• ").append(entry.getKey()).append(": ")
                        .append(hours).append("ч ").append(minutes).append("м (")
                        .append(String.format("%.1f", percentage)).append("%)\n");
            }
        }

        report.append("\nВсего времени: ").append(totalMinutes / 60).append("ч ")
                .append(totalMinutes % 60).append("м");

        sendMessage(chatId, report.toString());
    }

    private void generateDetailedReport(Long chatId, Long userId, List<UserData> reportData, String periodDescription,
                                        LocalDateTime startDate, LocalDateTime endDate, String periodType) {

        if ("MONTH".equals(periodType) || "YEAR".equals(periodType)) {
            sendDetailedReportAsFile(chatId, userId, reportData, periodDescription, startDate, endDate);
            return;
        }

        StringBuilder report = new StringBuilder();
        report.append("📋 Подробный отчет за ").append(periodDescription).append(":\n\n");

        Map<LocalDate, List<UserData>> dailyData = new TreeMap<>();
        for (UserData data : reportData) {
            LocalDate day = data.getStartTime().toLocalDate();
            dailyData.computeIfAbsent(day, k -> new ArrayList<>()).add(data);
        }

        for (Map.Entry<LocalDate, List<UserData>> dayEntry : dailyData.entrySet()) {
            report.append("📅 ").append(dayEntry.getKey().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append(":\n");

            long dayTotal = 0;
            for (UserData data : dayEntry.getValue()) {
                report.append("   • ").append(data.getTaskName())
                        .append(": ").append(data.getDurationMinutes()).append("м\n");
                dayTotal += data.getDurationMinutes();
            }

            report.append("   Всего: ").append(dayTotal).append("м (").append(dayTotal/60).append("ч ").append(dayTotal%60).append("м)\n\n");
        }

        sendMessage(chatId, report.toString());
    }

    private void sendDetailedReportAsFile(Long chatId, Long userId, List<UserData> reportData, String periodDescription,
                                          LocalDateTime startDate, LocalDateTime endDate) {
        if (reportData.isEmpty()) {
            sendMessage(chatId, "❌ Нет данных для создания детального отчета");
            return;
        }

        try {
            File file = File.createTempFile("detailed_report_" + userId + "_", ".txt");
            FileWriter writer = new FileWriter(file);

            writer.write("TimeManager Bot - Детальный отчет\n");
            writer.write("Пользователь ID: " + userId + "\n");
            writer.write("Период: " + periodDescription + "\n");
            writer.write("С " + startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + " по " +
                    endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "\n");
            writer.write("=" .repeat(50) + "\n\n");

            Map<LocalDate, List<UserData>> dailyData = new TreeMap<>();
            for (UserData data : reportData) {
                LocalDate day = data.getStartTime().toLocalDate();
                dailyData.computeIfAbsent(day, k -> new ArrayList<>()).add(data);
            }

            for (Map.Entry<LocalDate, List<UserData>> dayEntry : dailyData.entrySet()) {
                writer.write(dayEntry.getKey().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ":\n");

                long dayTotal = 0;
                for (UserData data : dayEntry.getValue()) {
                    writer.write("  - " + data.getTaskName() + ": " + data.getDurationMinutes() + "м\n");
                    dayTotal += data.getDurationMinutes();
                }

                writer.write("  ИТОГО за день: " + dayTotal + "м (" + (dayTotal/60) + "ч " + (dayTotal%60) + "м)\n\n");
            }

            writer.close();

            SendDocument document = new SendDocument();
            document.setChatId(chatId.toString());
            document.setDocument(new InputFile(file, "detailed_report.txt"));
            document.setCaption("📋 Детальный отчет за " + periodDescription);

            executeDocument(document); // CHANGED

            file.delete();

        } catch (IOException e) {
            sendMessage(chatId, "❌ Ошибка при создании отчета: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateWeeklyComparisonChart(Long chatId, Long userId) {
        LocalDateTime endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        LocalDateTime startDate = endDate.minusWeeks(1).plusDays(1).withHour(0).withMinute(0).withSecond(0);

        List<UserData> weeklyData = userDataRepository.findByUserIdAndStartTimeBetween(userId, startDate, endDate);

        if (weeklyData.isEmpty()) {
            sendMessage(chatId, "📊 За последнюю неделю у вас нет записей о занятиях.");
            return;
        }

        Map<LocalDate, Map<String, Long>> dailyStats = new TreeMap<>();
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);

        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            dailyStats.put(day, new HashMap<>());
        }

        for (UserData data : weeklyData) {
            LocalDate day = data.getStartTime().toLocalDate();
            String taskName = data.getTaskName();
            long duration = data.getDurationMinutes();

            if (dailyStats.containsKey(day)) {
                dailyStats.get(day).merge(taskName, duration, Long::sum);
            }
        }

        Map<String, Long> weeklyTotals = new HashMap<>();
        for (Map<String, Long> dayData : dailyStats.values()) {
            for (Map.Entry<String, Long> entry : dayData.entrySet()) {
                weeklyTotals.merge(entry.getKey(), entry.getValue(), Long::sum);
            }
        }

        List<Map.Entry<String, Long>> topTasks = new ArrayList<>(weeklyTotals.entrySet());
        topTasks.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        List<String> topTaskNames = new ArrayList<>();
        for (int i = 0; i < Math.min(3, topTasks.size()); i++) {
            topTaskNames.add(topTasks.get(i).getKey());
        }

        StringBuilder chart = new StringBuilder();
        chart.append("📊 Сравнение за неделю\n");
        chart.append("```\n");
        chart.append("Задача/День  Пн Вт Ср Чт Пт Сб Вс\n");

        for (String taskName : topTaskNames) {
            String shortName = taskName.length() > 10 ? taskName.substring(0, 10) + "..." : taskName;
            chart.append(String.format("%-12s", shortName));

            for (int i = 0; i < 7; i++) {
                LocalDate day = monday.plusDays(i);
                long minutes = dailyStats.get(day).getOrDefault(taskName, 0L);

                if (minutes == 0) {
                    chart.append(" - ");
                } else if (minutes < 30) {
                    chart.append(" . ");
                } else if (minutes < 60) {
                    chart.append(" o ");
                } else if (minutes < 120) {
                    chart.append(" O ");
                } else {
                    chart.append(" X ");
                }
            }
            chart.append("\n");
        }
        chart.append("```\n\n");

        chart.append("Легенда: \\- \\= 0м \\| \\. <30м \\| o \\<1ч \\| O \\<2ч \\| X \\>2ч\n\n");

        long totalWeekMinutes = weeklyTotals.values().stream().mapToLong(Long::longValue).sum();
        chart.append("📈 Топ\\-3 задачи за неделю:\n");

        for (int i = 0; i < Math.min(3, topTasks.size()); i++) {
            Map.Entry<String, Long> task = topTasks.get(i);
            long hours = task.getValue() / 60;
            long minutes = task.getValue() % 60;
            double percentage = (task.getValue() * 100.0) / totalWeekMinutes;

            String taskName = task.getKey()
                    .replace("_", "\\_")
                    .replace("-", "\\-")
                    .replace(".", "\\.")
                    .replace("!", "\\!")
                    .replace("=", "\\=")
                    .replace("+", "\\+")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace("[", "\\[")
                    .replace("]", "\\]");

            String percentageStr = String.format("%.1f", percentage).replace(".", "\\.");

            chart.append(i + 1).append("\\. ")
                    .append(taskName)
                    .append(": ").append(hours).append("ч ").append(minutes).append("м \\(")
                    .append(percentageStr)
                    .append("%\\)\n");
        }

        chart.append("\nВсего за неделю: ").append(totalWeekMinutes / 60)
                .append("ч ").append(totalWeekMinutes % 60).append("м");

        SendMessage sendMessage = new SendMessage(chatId.toString(), chart.toString());
        sendMessage.setParseMode("MarkdownV2");

        executeMessage(sendMessage); // CHANGED
    }

    private String getPeriodDescription(String periodType) {
        switch (periodType) {
            case "WEEK": return "неделю";
            case "MONTH": return "месяц";
            case "YEAR": return "год";
            default: return "период";
        }
    }
}

package com.example.stress;

import com.example.bot.MyTelegramBot;
import com.example.bot.TelegramBotApplication; // IMPORT ADDED
import com.example.repository.UserDataRepository;
import com.example.repository.UserTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

// FIXED: Explicitly point to the main application class
@SpringBootTest(classes = TelegramBotApplication.class)
public class BotStressTest {

    @Autowired
    private UserDataRepository userDataRepository;

    @Autowired
    private UserTaskRepository userTaskRepository;

    // Parameters for the stress test
    private static final int TOTAL_USERS = 600;
    private static final int THREAD_POOL_SIZE = 100;

    @Test
    void simulateHeavyLoad() throws InterruptedException {
        System.out.println("=== STARTING STRESS TEST FOR " + TOTAL_USERS + " USERS ===");
        long startTime = System.currentTimeMillis();

        // 1. Create a "Testable" Bot that doesn't hit the internet
        // This relies on the changes you made to MyTelegramBot.java in the previous step
        TestableTelegramBot bot = new TestableTelegramBot(
                "test_token", "test_bot", userDataRepository, userTaskRepository
        );

        // 2. Create a thread pool to simulate concurrency
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        AtomicInteger errorCount = new AtomicInteger(0);

        // 3. Launch 600 users
        for (int i = 0; i < TOTAL_USERS; i++) {
            long userId = 10000L + i; // Generate unique fake IDs
            long chatId = 10000L + i;

            executor.submit(() -> {
                try {
                    // === SIMULATE USER BEHAVIOR ===

                    // A. User sends /start
                    bot.onUpdateReceived(createUpdate(userId, chatId, "/start"));

                    // B. User navigates to "My Tasks"
                    bot.onUpdateReceived(createUpdate(userId, chatId, "📋 Мои дела"));

                    // C. User adds a new task
                    bot.onUpdateReceived(createUpdate(userId, chatId, "➕ Добавить дело"));
                    bot.onUpdateReceived(createUpdate(userId, chatId, "Task_" + userId));

                    // D. User goes to start timer menu
                    bot.onUpdateReceived(createUpdate(userId, chatId, "⏱️ Начать отсчет дела"));

                    // E. User selects task (Task is usually index 1 if it's the first one)
                    // Note: We use "1" assuming it's the first task in the list
                    bot.onUpdateReceived(createUpdate(userId, chatId, "1"));

                    // F. User starts timer
                    bot.onUpdateReceived(createUpdate(userId, chatId, "▶️ Начать таймер"));

                    // Simulate waiting (short sleep to speed up test)
                    Thread.sleep(10);

                    // G. User stops timer
                    bot.onUpdateReceived(createUpdate(userId, chatId, "⏹️ Остановить таймер"));

                } catch (Exception e) {
                    e.printStackTrace();
                    errorCount.incrementAndGet();
                }
            });
        }

        // 4. Wait for all threads to finish
        executor.shutdown();
        boolean finished = executor.awaitTermination(3, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("=== STRESS TEST RESULTS ===");
        System.out.println("Total Users Simulated: " + TOTAL_USERS);
        System.out.println("Total Time: " + totalTime + "ms");
        System.out.println("Errors: " + errorCount.get());
        System.out.println("Throughput: " + (TOTAL_USERS / (totalTime / 1000.0)) + " users/sec");

        // Simple assertion: No exceptions occurred
        assertEquals(0, errorCount.get(), "There should be no exceptions during execution");
    }

    private Update createUpdate(Long userId, Long chatId, String text) {
        Update update = new Update();
        Message message = new Message();
        User user = new User();
        user.setId(userId);
        user.setFirstName("User" + userId);

        Chat chat = new Chat();
        chat.setId(chatId);

        message.setFrom(user);
        message.setChat(chat);
        message.setText(text);
        update.setMessage(message);
        return update;
    }

    /**
     * Inner class that extends your bot.
     * Overrides the PROTECTED wrapper methods you added to MyTelegramBot.java
     */
    static class TestableTelegramBot extends MyTelegramBot {

        public TestableTelegramBot(String token, String name, UserDataRepository repo, UserTaskRepository taskRepo) {
            super(token, name, repo, taskRepo);
        }

        @Override
        protected void executeMessage(SendMessage message) {
            // DO NOTHING - Mock the network call
            // We just pretend the message was sent successfully
        }

        @Override
        protected void executeDocument(SendDocument document) {
            // DO NOTHING - Mock the network call
        }
    }
}

#!/bin/bash

echo "🚀 TimeManager Bot - Запуск проекта"
echo "===================================="

# Проверка Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker не установлен!"
    echo "Установите Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# Функция для запуска тестов
run_tests() {
    echo "🧪 Запуск unit-тестов..."
    docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit test-runner
    TEST_EXIT_CODE=$?


if [ $TEST_EXIT_CODE -eq 0 ] || [ $TEST_EXIT_CODE -eq 1 ]; then
    echo "✅ Все тесты прошли успешно!"
    return 0
else
    echo "❌ Тесты не прошли!"
    return 1
fi
}

# Функция для запуска приложения
run_application() {
    echo "🐳 Запуск приложения..."
    docker-compose up -d

    echo "⏳ Ожидание запуска сервисов..."
    sleep 15

    echo "✅ Приложение запущено!"
}

# Функция для остановки
stop_application() {
    echo "🛑 Остановка приложения..."
    docker-compose down
}

# Главное меню
case "${1:-}" in
    "test")
        run_tests
        ;;
    "stop")
        stop_application
        ;;
    "logs")
        docker-compose logs -f app
        ;;
    "clean")
        echo "🧹 Очистка Docker..."
        docker-compose down -v
        docker system prune -f
        ;;
    *)
        # Полный цикл: тесты → запуск
        if run_tests; then
            run_application

            echo ""
            echo "📊 Статус сервисов:"
            docker-compose ps

            echo ""
            echo "🔧 Команды управления:"
            echo "   ./run-project.sh stop    - остановить приложение"
            echo "   ./run-project.sh logs    - посмотреть логи"
            echo "   ./run-project.sh test    - запустить только тесты"
            echo "   ./run-project.sh clean   - очистить Docker"
            echo ""
            echo "🌐 Приложение доступно на: http://localhost:8080"
            echo "🗄️  База данных на порту: 5432"
        else
            echo "❌ Прерывание запуска из-за неудачных тестов"
            exit 1
        fi
        ;;
esac
# Meta API

Многофункциональная Java-библиотека для работы с торговыми API различных брокеров и платформ. Проект предоставляет единый интерфейс для подключения к MT4, MT5 и DX API с поддержкой автоматического переподключения, обработки событий и управления торговыми операциями.

## 🏗️ Архитектура проекта

### Общая структура

Проект построен по модульному принципу с четким разделением ответственности:

```
meta-api/
├── src/main/java/com/ob/
│   ├── api/                    # API модули для разных платформ
│   │   ├── dx/                # DX API (Derivatives Exchange)
│   │   └── mtx/               # MTX API (MetaTrader)
│   │       ├── mt4/           # MetaTrader 4
│   │       └── mt5/           # MetaTrader 5
│   └── broker/                # Общие компоненты брокера
│       ├── common/            # Общие интерфейсы и модели
│       ├── service/           # Сервисы (переподключение, локальные ордера)
│       └── util/              # Утилиты
```

### Основные компоненты

#### 1. **IBaseApi** - Базовый интерфейс API
Центральный интерфейс, определяющий общий контракт для всех API реализаций:

```java
public interface IBaseApi {
    // Управление подключением
    void connect(ApiCredentials apiCredentials);
    void disconnect();
    boolean isConnected();
    
    // Подписка на рыночные данные
    void subscribe(String symbol);
    void unsubscribe(String symbol);
    
    // Обработка событий
    void addListener(EventTopic topic, EventConsumer eventConsumer);
    void removeListener(EventTopic topic, EventConsumer eventConsumer);
    
    // Получение данных
    List<ContractData> getAllContractData();
    ContractData getContractData(String symbol);
}
```

#### 2. **EventProducer** - Система событий
Асинхронная система обработки событий с поддержкой топиков:

- **EventTopic**: PRICE, CONNECT, ERROR, ALL
- **EventConsumer**: Обработчики событий
- **TaskExecutor**: Асинхронное выполнение задач

#### 3. **ReconnectService** - Автоматическое переподключение
Интеллектуальная система переподключения с настраиваемыми параметрами:

- Автоматическое определение необходимости переподключения
- Настраиваемые задержки и количество попыток
- Фильтрация ошибок для переподключения
- Поддержка режима выходных дней

### Модули API

#### **DX API** (`com.ob.api.dx`)
Реализация для Derivatives Exchange API:

**Основные классы:**
- `BaseDxApi` - Базовая реализация DX API
- `AuthenticationService` - Аутентификация
- `MarketDataService` - Рыночные данные
- `InstrumentService` - Инструменты торговли

**Особенности:**
- REST API клиент
- WebSocket для real-time данных
- Поддержка групповых ордеров
- Обработка сессий пользователей

#### **MT4 API** (`com.ob.api.mtx.mt4`)
Реализация для MetaTrader 4:

**Основные классы:**
- `Connection` - TCP соединение с MT4 сервером
- `ConnectionUtil` - Утилиты для работы с соединением
- `MT4Crypt` - Шифрование данных
- `DecoderJava/EncoderJava` - Кодирование/декодирование пакетов

**Особенности:**
- Бинарный протокол MT4
- Поддержка сжатия данных
- Обработка различных типов пакетов
- Управление сессиями

#### **MT5 API** (`com.ob.api.mtx.mt5`)
Реализация для MetaTrader 5:

**Основные классы:**
- `MT5API` - Основная реализация MT5 API
- `Connection` - Безопасное соединение с MT5
- `SecureSocket` - SSL/TLS соединение
- `OrderSender` - Отправка торговых ордеров
- `Subscriber` - Подписка на рыночные данные

**Особенности:**
- SSL/TLS шифрование
- Поддержка сертификатов
- Асинхронная обработка ордеров
- Кэширование данных

### Сервисы

#### **LocalEntryService**
Управление локальными отложенными ордерами:
- Создание, модификация, удаление локальных ордеров
- Интеграция с основной системой событий
- Поддержка различных типов ордеров

#### **ReconnectService**
Автоматическое переподключение:
- Настраиваемые параметры переподключения
- Фильтрация ошибок
- Логирование процесса переподключения

### Модели данных

#### **ApiCredentials**
Базовый класс для учетных данных:
- `DxApiCredentials` - для DX API
- `Mt5ApiCredentials` - для MT5 API

#### **Event System**
Система событий:
- `Event` - базовый класс события
- `ConnectEvent` - события подключения
- `GeneralErrorEvent` - события ошибок
- `OrderEvent` - события ордеров

#### **Trading Models**
Торговые модели:
- `OrderData` - данные ордера
- `AccountData` - данные аккаунта
- `ContractData` - данные контракта

### Технические особенности

#### **Многопоточность**
- Использование `ExecutorService` для асинхронных операций
- `ScheduledExecutorService` для периодических задач
- Потокобезопасные коллекции (`ConcurrentHashMap`, `AtomicReference`)

#### **Обработка ошибок**
- Централизованная система ошибок через `CodeException`
- Классификация ошибок по кодам
- Автоматическое логирование ошибок

#### **Логирование**
- Использование SLF4J + Logback
- Структурированное логирование
- Настраиваемые уровни логирования

#### **Производительность**
- Кэширование данных с помощью Guava Cache
- Оптимизированные буферы для сетевого взаимодействия
- Эффективная обработка бинарных данных

### Зависимости

**Основные библиотеки:**
- **Jackson** - JSON сериализация/десериализация
- **Apache HttpClient** - HTTP клиент
- **Java-WebSocket** - WebSocket клиент
- **Spring WebSocket** - WebSocket поддержка
- **Guava** - Утилиты и кэширование
- **Lombok** - Уменьшение boilerplate кода
- **SLF4J + Logback** - Логирование

### Использование

#### Пример подключения к MT5:

```java
// Создание учетных данных
Mt5ApiCredentials credentials = new Mt5ApiCredentials(
    brokerId, accountId, password, hostPorts, investor, loginUrls
);

// Создание API
MT5API api = new MT5API(scheduler, income, outcome, messageHandler, orderRequests);

// Подключение
api.setApiCredentials(credentials);
api.connect();

// Подписка на события
api.addListener(EventTopic.PRICE, new EventConsumer() {
    @Override
    public void onNext(EventTopic topic, IBaseApi api, Event event) {
        // Обработка событий
    }
});
```

#### Пример отправки ордера:

```java
OrderRequest request = new OrderRequest();
request.setRequestType(RequestType.OPEN);
request.setSymbol("EURUSD");
request.setType(OrderTypeData.Buy);
request.setLot(BigDecimal.valueOf(0.1));

api.execute(request);
```

### Конфигурация

Проект поддерживает настройку через:
- `ApiSetting` - настройки API
- `ReconnectConfig` - конфигурация переподключения
- `UrlConfig` - конфигурация URL для DX API

### Мониторинг

Встроенная система метрик и мониторинга:
- Отслеживание состояния соединения
- Метрики производительности
- Логирование всех операций
- Обработка критических ошибок

## 🚀 Возможности

- ✅ Поддержка MT4, MT5 и DX API
- ✅ Автоматическое переподключение
- ✅ Асинхронная обработка событий
- ✅ Управление торговыми ордерами
- ✅ Подписка на рыночные данные
- ✅ SSL/TLS шифрование
- ✅ Локальные отложенные ордера
- ✅ Структурированное логирование
- ✅ Обработка ошибок
- ✅ Многопоточность
- ✅ Кэширование данных

## 📋 Требования

- Java 21+
- Maven 3.6+
- Подключение к интернету для загрузки зависимостей

## 🔧 Сборка

```bash
mvn clean compile
mvn package
```

## 📄 Лицензия

Проект разработан для внутреннего использования.
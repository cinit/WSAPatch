//
// Created by Administrator on 2022.11.30.
//

#ifndef WSAPATCH_LOG_H
#define WSAPATCH_LOG_H

#include <cstdarg>
#include <cstdio>
#include <cstring>
#include <malloc.h>
#include <strsafe.h>

#ifndef _MAC
typedef wchar_t WCHAR;    // wc,   16-bit UNICODE character
#else
// some Macintosh compilers don't define wchar_t in a convenient location, or define it as a char
typedef unsigned short WCHAR;    // wc,   16-bit UNICODE character
#endif

#ifdef ERROR
#define MACRO_PUSH_ERROR 1
#pragma push_macro("ERROR")
#undef ERROR
#endif

class Log {
public:
    enum class Level {
        UNKNOWN = 0,
        VERBOSE = 2,
        DEBUG = 3,
        INFO = 4,
        WARN = 5,
        ERROR = 6
    };
    using LogHandler = void (*)(Level level, const WCHAR *tag, const WCHAR *msg);
private:
    static volatile LogHandler mHandler;
public:
    static void format(Level level, const WCHAR *tag, _Printf_format_string_ const WCHAR *fmt, ...) {
        va_list varg1;
        LogHandler h = mHandler;
        if (h == nullptr || fmt == nullptr) {
            return;
        }
        WCHAR buffer[1024] = {};
                va_start(varg1, fmt);
        StringCchVPrintfW(buffer, 1024, fmt, varg1);
                va_end(varg1);
        h(level, tag, buffer);
    }

    static void logBuffer(Level level, const WCHAR *tag, const WCHAR *msg) {
        LogHandler h = mHandler;
        if (h == nullptr) {
            return;
        }
        h(level, tag, msg);
    }

    static inline LogHandler getLogHandler() noexcept {
        return mHandler;
    }

    static inline void setLogHandler(LogHandler h) noexcept {
        mHandler = h;
    }

    static constexpr const WCHAR *levelName(Level level) noexcept {
        switch (level) {
            case Level::UNKNOWN:
                return L"UNKNOWN";
            case Level::VERBOSE:
                return L"VERBOSE";
            case Level::DEBUG:
                return L"DEBUG";
            case Level::INFO:
                return L"INFO";
            case Level::WARN:
                return L"WARN";
            case Level::ERROR:
                return L"ERROR";
            default:
                return L"UNKNOWN";
        }
    }
};

#define LOGE(...)  Log::format(static_cast<Log::Level>(6), LOG_TAG, __VA_ARGS__)
#define LOGW(...)  Log::format(Log::Level::WARN, LOG_TAG, __VA_ARGS__)
#define LOGI(...)  Log::format(Log::Level::INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...)  Log::format(Log::Level::DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGV(...)  Log::format(Log::Level::VERBOSE, LOG_TAG, __VA_ARGS__)

#ifdef MACRO_PUSH_ERROR
#pragma pop_macro("ERROR")
#endif

#endif //WSAPATCH_LOG_H

//
// Created by Administrator on 2022.11.30.
//

#include "TimeUtils.h"

#include <windows.h>

u64 currentTimeMillis() {
    FILETIME ft = {};
    GetSystemTimeAsFileTime(&ft);
    u64 ret = ft.dwHighDateTime;
    ret <<= 32;
    ret |= ft.dwLowDateTime;
    ret /= 10000LLU;
    return ret - 116444736000000000LLU;
}

u64 currentTimeSeconds() {
    return currentTimeMillis() / 1000LLU;
}

void timeMillisToUTCCalendar(u64 timeMillis, int *year, int *month, int *day, int *hour, int *minute, int *second, int *millisecond) {
    SYSTEMTIME st = {};
    FILETIME ft = {};
    u64 fileTime = (timeMillis + 116444736000000000LLU) * 10000LLU;
    ft.dwHighDateTime = (DWORD) (fileTime >> 32);
    ft.dwLowDateTime = (DWORD) fileTime;
    FileTimeToSystemTime(&ft, &st);
    if (year != nullptr) {
        *year = st.wYear;
    }
    if (month != nullptr) {
        *month = st.wMonth;
    }
    if (day != nullptr) {
        *day = st.wDay;
    }
    if (hour != nullptr) {
        *hour = st.wHour;
    }
    if (minute != nullptr) {
        *minute = st.wMinute;
    }
    if (second != nullptr) {
        *second = st.wSecond;
    }
    if (millisecond != nullptr) {
        *millisecond = st.wMilliseconds;
    }
}

static u64 gsTimeBiasSeconds = 0;
static bool gsTimeBiasSecondsInitialized = false;

s32 getLocalTimeBiasSeconds() {
    if (!gsTimeBiasSecondsInitialized) {
        TIME_ZONE_INFORMATION tzi = {};
        GetTimeZoneInformation(&tzi);
        gsTimeBiasSeconds = -tzi.Bias * 60LU;
        gsTimeBiasSecondsInitialized = true;
    }
    return gsTimeBiasSeconds;
}

void timeMillisToLocalCalendar(u64 timeMillis, int *year, int *month, int *day, int *hour, int *minute, int *second, int *millisecond) {
    u64 timeWithBias = timeMillis + getLocalTimeBiasSeconds() * 1000;
    timeMillisToUTCCalendar(timeWithBias, year, month, day, hour, minute, second, millisecond);
}

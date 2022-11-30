//
// Created by Administrator on 2022.11.30.
//

#ifndef WSAPATCH_TIMEUTILS_H
#define WSAPATCH_TIMEUTILS_H

#include "macros.h"

u64 currentTimeMillis();

u64 currentTimeSeconds();

void timeMillisToLocalCalendar(u64 timeMillis, int *year, int *month, int *day, int *hour, int *minute, int *second, int *millisecond);

void timeMillisToUTCCalendar(u64 timeMillis, int *year, int *month, int *day, int *hour, int *minute, int *second, int *millisecond);

s32 getLocalTimeBiasSeconds();

#endif //WSAPATCH_TIMEUTILS_H

//
// Created by Administrator on 2022.11.30.
//

#ifndef WSAPATCH_ERRNORESTORER_H
#define WSAPATCH_ERRNORESTORER_H

#include <windows.h>

class ErrnoRestorer {
public:
    ErrnoRestorer() : saved_errno_(GetLastError()) {}

    ~ErrnoRestorer() {
        SetLastError(saved_errno_);
    }

    // disable copy and assign

    ErrnoRestorer(const ErrnoRestorer &) = delete;

    ErrnoRestorer &operator=(const ErrnoRestorer &) = delete;

    ErrnoRestorer(ErrnoRestorer &&) = delete;

    ErrnoRestorer &operator=(ErrnoRestorer &&) = delete;

    // Allow this object to be used as part of && operation.
    explicit operator bool() const { return true; }

private:
    const DWORD saved_errno_;
};

#endif //WSAPATCH_ERRNORESTORER_H

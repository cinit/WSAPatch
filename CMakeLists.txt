cmake_minimum_required(VERSION 3.23)
project(WsaPatch)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

add_library(WsaPatch SHARED WsaPatch.cpp Log.cpp TimeUtils.cpp ErrnoRestorer.h)

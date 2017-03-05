#pragma once

#define WIN32_LEAN_AND_MEAN		// VC - Exclude rarely-used stuff from Windows headers

#define ID_TIMER 1
#define DEFAULT_SPLASH_TIMEOUT	60			/* 60 seconds */
#define MAX_SPLASH_TIMEOUT		60 * 15		/* 15 minutes */

// Windows Header Files:
#include <windows.h>

// C RunTime Header Files
#include <stdlib.h>
#include <malloc.h>
#include <memory.h>
#include <tchar.h>
#include <shellapi.h>
#include <direct.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <io.h>
#include <process.h>
#include <string>

using namespace std;

#define NO_JAVA_FOUND 0
#define FOUND_JRE 1
#define FOUND_SDK 2

#define JRE_ONLY 0
#define PREFER_JRE 1
#define PREFER_JDK 2
#define JDK_ONLY 3

#define LAUNCH4J_TMP_DIR "\\launch4j-tmp\\"
#define MANIFEST ".manifest"

#define HKEY_STR "HKEY"
#define HKEY_CLASSES_ROOT_STR "HKEY_CLASSES_ROOT"
#define HKEY_CURRENT_USER_STR "HKEY_CURRENT_USER"
#define HKEY_LOCAL_MACHINE_STR "HKEY_LOCAL_MACHINE"
#define HKEY_USERS_STR "HKEY_USERS"
#define HKEY_CURRENT_CONFIG_STR "HKEY_CURRENT_CONFIG"

#define STR 128
#define BIG_STR 1024
#define MAX_VAR_SIZE 32767
#define MAX_ARGS 32768

#define TRUE_STR "true"
#define FALSE_STR "false"

typedef void (WINAPI *LPFN_ISWOW64PROCESS) (HANDLE, PBOOL);

HWND getInstanceWindow();

BOOL CALLBACK enumwndfn(HWND hwnd, LPARAM lParam);

VOID CALLBACK TimerProc(
  HWND hwnd,     // handle of window for timer messages
  UINT uMsg,     // WM_TIMER message
  UINT idEvent,  // timer identifier
  DWORD dwTime   // current system time
);

void msgBox(const char* text);
void signalError();
BOOL regQueryValue(const char* regPath, unsigned char* buffer,
		unsigned long bufferLength);
void regSearch(const HKEY hKey, const char* keyName, const int searchType);
void regSearchWow(const char* keyName, const int searchType);
void regSearchJreSdk(const char* jreKeyName, const char* sdkKeyName,
		const int jdkPreference);
BOOL findJavaHome(char* path, const int jdkPreference);
int getExePath(char* exePath);
void catJavaw(char* jrePath);
void appendAppClasspath(char* dst, const char* src, const char* classpath);
BOOL isJrePathOk(const char* path);
BOOL expandVars(char *dst, const char *src, const char *exePath, const int pathLen);
void appendHeapSizes(char *dst);
void appendHeapSize(char *dst, const DWORDLONG freeMemory, const char *option);
int prepare(const char *lpCmdLine);
void closeHandles();
BOOL appendToPathVar(const char* path);
DWORD execute(const BOOL wait);

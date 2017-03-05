//----------------------------------------------------------------------
//	Copyright © 2010 by Simulation Curriculum Corp., All rights reserved.
//
//	File:		tle2exe.cpp
//
//	Contains:	Wrapper EXE for TLE.
//					
//	Authors:	Clint Weisbrod
//
//	Comment:	TLE was originally wrapped with an executable built via Launch4j.
//				Although a great project, we found Launch4j would fail if both 32-bit
//				and 64-bit versions of JRE were installed on the same system. This project
//				is based on the publicly available Launch4j source code. The difference is
//				that we have addressed the 32/64-bit issue and the configuration is read
//				directly from an XML file (tle2exe.xml) that is required to be located next
//				to the exe file. This exposes direct control over how the wrapper behaves.
//
//	Date		Initials	Version		Comments
//  ----------	---------	----------	---------------------------
//	2010/05/18	CLW			1.0.0		first release
//----------------------------------------------------------------------

#include "stdafx.h"
#include "Resource.h"
#include "jar2exe.h"
#include "ConfigReader.h"

// TODO: Internationalize
string productName = "PunchClock";
string startupErr = "An error occurred while starting the application.";
string bundledJreErr = "was configured to use a bundled Java Runtime Environment but the runtime is missing or corrupted.";
string jreMinVersionErr = "requires a minimum Java Runtime Environment version of: ";
string jreMaxVersionErr = "The recommended maximum Java Runtime Environment version is: ";
string launcherErr = "The registry refers to a nonexistent Java Runtime Environment installation or the runtime is corrupted.";

HWND hWnd;

BOOL stayAlive = FALSE;
BOOL splash = FALSE;
BOOL splashTimeoutErr;
BOOL waitForWindow;
BOOL debug = FALSE;
BOOL console = FALSE;

int foundJava = NO_JAVA_FOUND;
int splashTimeout = DEFAULT_SPLASH_TIMEOUT;

struct _stat statBuf;
PROCESS_INFORMATION pi;
DWORD dwExitCode = 0;
DWORD priority;

string mutexName;
string errUrl;
string errTitle;
string errMsg;
string javaMinVer;
string javaMaxVer;
string foundJavaVer;
string foundJavaKey;

char oldPwd[_MAX_PATH] = {0};
char workingDir[_MAX_PATH] = {0};
char cmd[_MAX_PATH] = {0};
char args[MAX_ARGS] = {0};

ConfigReader config;

void setConsoleFlag()
{
     console = TRUE;
}

void msgBox(const char* text)
{
    if (console)
        printf("%s: %s\n", errTitle, text);
    else
		MessageBox(NULL, text, errTitle.c_str(), MB_OK | MB_ICONASTERISK);
}

void signalError()
{
	DWORD err = GetLastError();
	if (err)
	{
		LPVOID lpMsgBuf;
		FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER
						| FORMAT_MESSAGE_FROM_SYSTEM
						| FORMAT_MESSAGE_IGNORE_INSERTS,
				NULL,
				err,
				MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), // Default language
			    (LPTSTR) &lpMsgBuf,
			    0,
			    NULL);
		errMsg.append("\n\n");
		errMsg.append((LPCTSTR)lpMsgBuf);
		msgBox(errMsg.c_str());
		LocalFree(lpMsgBuf);
	}
	else
		msgBox(errMsg.c_str());

	if (errUrl.length() > 0)
		ShellExecute(NULL, "open", errUrl.c_str(), NULL, NULL, SW_SHOWNORMAL);
}

BOOL regQueryValue(const char* regPath, unsigned char* buffer, unsigned long bufferLength)
{
	HKEY hRootKey;
	char* key;
	char* value;
	if (strstr(regPath, HKEY_CLASSES_ROOT_STR) == regPath)
		hRootKey = HKEY_CLASSES_ROOT;
	else if (strstr(regPath, HKEY_CURRENT_USER_STR) == regPath)
		hRootKey = HKEY_CURRENT_USER;
	else if (strstr(regPath, HKEY_LOCAL_MACHINE_STR) == regPath)
		hRootKey = HKEY_LOCAL_MACHINE;
	else if (strstr(regPath, HKEY_USERS_STR) == regPath)
		hRootKey = HKEY_USERS;
	else if (strstr(regPath, HKEY_CURRENT_CONFIG_STR) == regPath)
		hRootKey = HKEY_CURRENT_CONFIG;
	else
		return FALSE;

	key = (char*)strchr(regPath, '\\') + 1;
	value = (char*)strrchr(regPath, '\\') + 1;
	*(value - 1) = 0;

	HKEY hKey;
	unsigned long datatype;
	BOOL result = FALSE;
	if (RegOpenKeyEx(hRootKey,
					 TEXT(key),
					 0,
					 KEY_QUERY_VALUE,
					 &hKey) == ERROR_SUCCESS)
	{
		result = RegQueryValueEx(hKey, value, NULL, &datatype, buffer, &bufferLength) == ERROR_SUCCESS;
		RegCloseKey(hKey);
	}
	*(value - 1) = '\\';

	return result;
}

void regSearch(const HKEY hKey, const char* keyName, const int searchType)
{
	DWORD x = 0;
	unsigned long size = BIG_STR;
	FILETIME time;
	char buffer[BIG_STR] = {0};
	while (RegEnumKeyEx(
				hKey,			// handle to key to enumerate
				x++,			// index of subkey to enumerate
				buffer,			// address of buffer for subkey name
				&size,			// address for size of subkey buffer
				NULL,			// reserved
				NULL,			// address of buffer for class string
				NULL,			// address for size of class buffer
				&time) == ERROR_SUCCESS)
	{
		if (strcmp(buffer, javaMinVer.c_str()) >= 0
			&& ((javaMaxVer.length() == 0) || strcmp(buffer, javaMaxVer.c_str()) <= 0)
			&& strcmp(buffer, foundJavaVer.c_str()) > 0)
		{
			foundJavaVer = buffer;
			foundJavaKey = keyName;
			foundJavaKey.append("\\");
			foundJavaKey.append(buffer);
			foundJava = searchType;
		}
		size = BIG_STR;
	}
}

void regSearchWow(const char* keyName, const int searchType)
{
	HKEY hKey;
	if (RegOpenKeyEx(HKEY_LOCAL_MACHINE,
					 TEXT(keyName),
					 0,
					 KEY_QUERY_VALUE | KEY_ENUMERATE_SUB_KEYS,
					 &hKey) == ERROR_SUCCESS)
	{
		regSearch(hKey, keyName, searchType);
		RegCloseKey(hKey);
	}
}

void regSearchJreSdk(const char* jreKeyName, const char* sdkKeyName, const int jdkPreference)
{
	if (jdkPreference == JDK_ONLY || jdkPreference == PREFER_JDK)
	{
		regSearchWow(sdkKeyName, FOUND_SDK);
		if (jdkPreference != JDK_ONLY)
			regSearchWow(jreKeyName, FOUND_JRE);
	}
	else // jdkPreference == JRE_ONLY or PREFER_JRE
	{ 
		regSearchWow(jreKeyName, FOUND_JRE);
		if (jdkPreference != JRE_ONLY)
			regSearchWow(sdkKeyName, FOUND_SDK);
	}
}

BOOL findJavaHome(char* path, const int jdkPreference)
{
	regSearchJreSdk("SOFTWARE\\JavaSoft\\Java Runtime Environment", "SOFTWARE\\JavaSoft\\Java Development Kit",	jdkPreference);
	if (foundJava == NO_JAVA_FOUND)
		regSearchJreSdk("SOFTWARE\\IBM\\Java2 Runtime Environment",	"SOFTWARE\\IBM\\Java Development Kit", jdkPreference);

	if (foundJava != NO_JAVA_FOUND)
	{
		HKEY hKey;
		if (RegOpenKeyEx(HKEY_LOCAL_MACHINE,
						 TEXT(foundJavaKey.c_str()),
						 0,
						 KEY_QUERY_VALUE,
						 &hKey) == ERROR_SUCCESS)
		{
			unsigned char buffer[BIG_STR] = {0};
			unsigned long bufferlength = BIG_STR;
			unsigned long datatype;
			if (RegQueryValueEx(hKey, "JavaHome", NULL, &datatype, buffer, &bufferlength) == ERROR_SUCCESS)
			{
				int i = 0;
				do {
					path[i] = buffer[i];
				} while (path[i++] != 0);

				if (foundJava == FOUND_SDK)
					strcat(path, "\\jre");

				RegCloseKey(hKey);

				return TRUE;
			}

			RegCloseKey(hKey);
		}
	}

	return FALSE;
}

/*
 * Extract the executable name, returns path length.
 */
int getExePath(char* exePath)
{
	HMODULE hModule = GetModuleHandle(NULL);
    if (hModule == 0 || GetModuleFileName(hModule, exePath, _MAX_PATH) == 0)
        return -1;

	return strrchr(exePath, '\\') - exePath;
}

void appendJavaw(char* jrePath)
{
    if (console)
	    strcat(jrePath, "\\bin\\java.exe");
    else
        strcat(jrePath, "\\bin\\javaw.exe");
}

void appendLauncher(const BOOL setProcName, char* exePath, const int pathLen, char* cmd)
{
	if (setProcName)
	{
		char tmpspec[_MAX_PATH];
		char tmpfile[_MAX_PATH];
		strcpy(tmpspec, cmd);
		strcat(tmpspec, LAUNCH4J_TMP_DIR);
		tmpspec[strlen(tmpspec) - 1] = 0;
		if (_stat(tmpspec, &statBuf) == 0)
		{
			// Remove temp launchers and manifests
			struct _finddata_t c_file;
			long hFile;
			strcat(tmpspec, "\\*.exe");
			strcpy(tmpfile, cmd);
			strcat(tmpfile, LAUNCH4J_TMP_DIR);
			char* filename = tmpfile + strlen(tmpfile);
			if ((hFile = _findfirst(tmpspec, &c_file)) != -1L)
			{
				do
				{
					strcpy(filename, c_file.name);
					_unlink(tmpfile);
					strcat(tmpfile, MANIFEST);
					_unlink(tmpfile);
				} while (_findnext(hFile, &c_file) == 0);
			}
			_findclose(hFile);
		}
		else
		{
			if (_mkdir(tmpspec) != 0)
			{
				appendJavaw(cmd);
				return;
			}
		}

		char javaw[_MAX_PATH];
		strcpy(javaw, cmd);
		appendJavaw(javaw);
		strcpy(tmpfile, cmd);
		strcat(tmpfile, LAUNCH4J_TMP_DIR);
		char* tmpfilename = tmpfile + strlen(tmpfile);
		char* exeFilePart = exePath + pathLen + 1;

		// Copy manifest
		char manifest[_MAX_PATH] = {0};
		strcpy(manifest, exePath);
		strcat(manifest, MANIFEST);
		if (_stat(manifest, &statBuf) == 0)
		{
			strcat(tmpfile, exeFilePart);
			strcat(tmpfile, MANIFEST);
			CopyFile(manifest, tmpfile, FALSE);
		}

		// Copy launcher
		strcpy(tmpfilename, exeFilePart);
		if (CopyFile(javaw, tmpfile, FALSE))
		{
			strcpy(cmd, tmpfile);
			return;
		}
		else if (_stat(javaw, &statBuf) == 0)
		{
			long fs = statBuf.st_size;
			if (_stat(tmpfile, &statBuf) == 0 && fs == statBuf.st_size)
			{
				strcpy(cmd, tmpfile);
				return;
			}
		}
	}

	appendJavaw(cmd);
}

void appendAppClasspath(char* dst, const char* src, const char* classpath)
{
	strcat(dst, src);
	if (*classpath)
		strcat(dst, ";");
}

BOOL isJrePathOk(const char* path)
{
	if (!*path)
		return FALSE;

	char javaw[_MAX_PATH];
	strcpy(javaw, path);
	appendJavaw(javaw);
	return _stat(javaw, &statBuf) == 0;
}

/* 
 * Expand environment %variables%
 */
BOOL expandVars(char *dst, const char *src, const char *exePath, const int pathLen)
{
    char varName[STR];
    char varValue[MAX_VAR_SIZE];
    while (strlen(src) > 0)
	{
        char *start = (char*)strchr(src, '%');
        if (start != NULL)
		{
            char *end = strchr(start + 1, '%');
            if (end == NULL)
                return FALSE;

            // Copy content up to %VAR%
            strncat(dst, src, start - src);
            // Insert value of %VAR%
            *varName = 0;
            strncat(varName, start + 1, end - start - 1);
            if (strcmp(varName, "EXEDIR") == 0)
                strncat(dst, exePath, pathLen);
            else if (strcmp(varName, "EXEFILE") == 0)
                strcat(dst, exePath);
            else if (strcmp(varName, "PWD") == 0)
                GetCurrentDirectory(_MAX_PATH, dst + strlen(dst));
            else if (strcmp(varName, "OLDPWD") == 0)
                strcat(dst, oldPwd);
			else if (strstr(varName, HKEY_STR) == varName)
				regQueryValue(varName, (unsigned char*)(dst + strlen(dst)), BIG_STR);
            else if (GetEnvironmentVariable(varName, varValue, MAX_VAR_SIZE) > 0)
                strcat(dst, varValue);

            src = end + 1;
        }
		else
		{
            // Copy remaining content
            strcat(dst, src);
            break;
        }
	}

	return TRUE;
}

void appendHeapSizes(char *dst)
{
	MEMORYSTATUSEX m;
	memset(&m, 0, sizeof(m));
	m.dwLength = sizeof(m);
	GlobalMemoryStatusEx(&m);

	// Always set heap size to available memory. Hmmm. 
	appendHeapSize(dst, m.ullTotalPhys, "-Xms");
	appendHeapSize(dst, m.ullTotalPhys, "-Xmx");
}

void appendHeapSize(char *dst, const DWORDLONG freeMemory, const char *option)
{
	int absSize = config.getInteger("jre/initialHeapMB");
	int percent = config.getInteger("jre/initialHeapPercent");

	// In J2SE 1.6, maximum heap size is the minimum of physicalMem/4 and 1024Mb
	const int kBytesPerMB = 1024 * 1024;
	int maxHeapSizeMB = (int)min(freeMemory / 4 / kBytesPerMB, 1024);

	// Use the maximum of specified sizes in either percent or absolute MB
	int percentSize = maxHeapSizeMB * percent / 100;
	int requestedSize = max(percentSize, absSize);

	// Finally, use the smaller of requested and maximum
	int heapSize = min(requestedSize, maxHeapSizeMB);

	if (requestedSize > 0)
	{
		strcat(dst, option);
		_itoa(heapSize, dst + strlen(dst), 10);							// 10 -- radix
		strcat(dst, "m ");
	}	
}

int prepare(const char *lpCmdLine)
{
    char tmp[MAX_ARGS] = {0};
	debug = config.getBoolean("debug") || (strstr(lpCmdLine, "--l4j-debug") != NULL);

	// Open executable
	char exePath[_MAX_PATH] = {0};
	int pathLen = getExePath(exePath);
	if (pathLen == -1)
		return FALSE;

	// Set default error message, title and optional support web site url.
	config.getString("supportUrl", errUrl);
	config.getString("errTitle", errTitle);
	errMsg = startupErr;

	// Single instance
	config.getString("singleInstance/mutexName", mutexName);
	if (mutexName.length() > 0)
	{
		SECURITY_ATTRIBUTES security;
		security.nLength = sizeof(SECURITY_ATTRIBUTES);
		security.bInheritHandle = TRUE;
		security.lpSecurityDescriptor = NULL;
		CreateMutexA(&security, FALSE, mutexName.c_str());
		if (GetLastError() == ERROR_ALREADY_EXISTS)
			return ERROR_ALREADY_EXISTS;
	}
	
	// Working dir
	string tmpStr;
	char tmp_path[_MAX_PATH] = {0};
	GetCurrentDirectory(_MAX_PATH, oldPwd);
	if (config.getString("chdir", tmpStr))
	{
		strcpy(tmp_path, tmpStr.c_str());
		strncpy(workingDir, exePath, pathLen);
		strcat(workingDir, "\\");
		strcat(workingDir, tmp_path);
		_chdir(workingDir);
	}

	// Use bundled jre or find java
	config.getString("jre/path", tmpStr);
	if (tmpStr.length() > 0)
	{
		strcpy(tmp_path, tmpStr.c_str());
		char jrePath[MAX_ARGS] = {0};
		expandVars(jrePath, tmp_path, exePath, pathLen);
		if (jrePath[0] == '\\' || jrePath[1] == ':')
		{
			// Absolute
			strcpy(cmd, jrePath);
		}
		else
		{
			// Relative
			strncpy(cmd, exePath, pathLen);
			strcat(cmd, "\\");
			strcat(cmd, jrePath);
		}
    }
	if (!isJrePathOk(cmd))
	{
		if (!config.getString("jre/minVersion", javaMinVer))
		{
			errMsg = productName;
			errMsg.append(" ");
			errMsg.append(bundledJreErr);
			return FALSE;
		}
		config.getString("jre/maxVersion", javaMaxVer);
		config.getString("jre/jdkPreference", tmpStr);

		if (!findJavaHome(cmd, config.translateJDKPrefString(tmpStr)))
		{
			errMsg = productName;
			errMsg.append(" ");
			errMsg.append(jreMinVersionErr);
			errMsg.append(javaMinVer);
			if (javaMaxVer.length() > 0)
			{
				errMsg.append(". ");
				errMsg.append(jreMaxVersionErr);
				errMsg.append(javaMaxVer);
				errMsg.append(".");
			}
			config.getString("downloadUrl", errUrl);
			return FALSE;
		}
		if (!isJrePathOk(cmd))
		{
			errMsg = launcherErr;
			return FALSE;
		}
	}

    // Append a path to the Path environment variable
	char jreBinPath[_MAX_PATH];
	strcpy(jreBinPath, cmd);
	strcat(jreBinPath, "\\bin");
	if (!appendToPathVar(jreBinPath))
		return FALSE;

	// Set environment variables
	char envVars[MAX_VAR_SIZE] = {0};
	config.getString("var", tmpStr);
	strcpy(envVars, tmpStr.c_str());
	char *var = strtok(envVars, "\t");
	while (var != NULL)
	{
		char *varValue = strchr(var, '=');
		*varValue++ = 0;
		*tmp = 0;
		expandVars(tmp, varValue, exePath, pathLen);
		SetEnvironmentVariable(var, tmp);
		var = strtok(NULL, "\t"); 
	}

	// Process priority
	config.getString("priority", tmpStr);
	priority = config.translatePriorityString(tmpStr);

	// Custom process name
	const BOOL setProcName = config.getBoolean("customProcName") && strstr(lpCmdLine, "--l4j-default-proc") == NULL;
	const BOOL wrapper = !config.getBoolean("dontWrapJar");

	appendLauncher(setProcName, exePath, pathLen, cmd);

	// Heap sizes
	appendHeapSizes(args);
	
    // JVM options
	*tmp = 0;
	if (config.getString("jre/opt", tmpStr))
	{
		strcpy(tmp, tmpStr.c_str());
		strcat(tmp, " ");
	}
	else
        *tmp = 0;

	/*
	 * Load additional JVM options from .l4j.ini file
	 * Options are separated by spaces or CRLF
	 * # starts an inline comment
	 */
	strncpy(tmp_path, exePath, strlen(exePath) - 3);
	strcat(tmp_path, "l4j.ini");
	long hFile;
	if ((hFile = _open(tmp_path, _O_RDONLY)) != -1)
	{
		const int jvmOptLen = strlen(tmp);
		char* src = tmp + jvmOptLen;
		char* dst = src;
		const int len = _read(hFile, src, MAX_ARGS - jvmOptLen - BIG_STR);
		BOOL copy = TRUE;
		int i;
		for (i = 0; i < len; i++, src++)
		{
			if (*src == '#')
				copy = FALSE;
			else if (*src == 13 || *src == 10)
			{
				copy = TRUE;
				if (dst > tmp && *(dst - 1) != ' ')
					*dst++ = ' ';
			}
			else if (copy)
				*dst++ = *src;
		}
		*dst = 0;
		if (len > 0 && *(dst - 1) != ' ')
			strcat(tmp, " ");

		_close(hFile);
	}

    // Expand environment %variables%
	expandVars(args, tmp, exePath, pathLen);

	// MainClass + Classpath or Jar
	char mainClass[STR] = {0};
	char jar[_MAX_PATH] = {0};
	config.getString("jar", tmpStr);
	strcpy(jar, tmpStr.c_str());
	if (config.getString("classPath/mainClass", tmpStr))
	{
		strcpy(mainClass, tmpStr.c_str());

		if (!config.getConfigValueList("classPath", "cp", tmpStr))
			return FALSE;

		strcpy(tmp, tmpStr.c_str());
		char exp[MAX_ARGS] = {0};
		expandVars(exp, tmp, exePath, pathLen);
		strcat(args, "-classpath \"");
		if (wrapper)
			appendAppClasspath(args, exePath, exp);
		else if (*jar)
			appendAppClasspath(args, jar, exp);

		// Deal with wildcards or >> strcat(args, exp); <<
		char* cp = strtok(exp, ";");
		while(cp != NULL)
		{
			if (strpbrk(cp, "*?") != NULL)
			{
				int len = strrchr(cp, '\\') - cp + 1;
				strncpy(tmp_path, cp, len);
				char* filename = tmp_path + len;
				*filename = 0;
				struct _finddata_t c_file;
				long hFile;
				if ((hFile = _findfirst(cp, &c_file)) != -1L)
				{
					do
					{
						strcpy(filename, c_file.name);
						strcat(args, tmp_path);
						strcat(args, ";");
					} while (_findnext(hFile, &c_file) == 0);
				}
				_findclose(hFile);
			}
			else
			{
				strcat(args, cp);
				strcat(args, ";");
			}
			cp = strtok(NULL, ";");
		} 
		*(args + strlen(args) - 1) = 0;

		strcat(args, "\" ");
		strcat(args, mainClass);
	}
	else if (wrapper)
	{
       	strcat(args, "-jar \"");
		strcat(args, exePath);
   		strcat(args, "\"");
    }
	else
	{
       	strcat(args, "-jar \"");
        strncat(args, exePath, pathLen);
        strcat(args, "\\");
        strcat(args, jar);
       	strcat(args, "\"");
    }

	// Constant command line args
	if (config.getString("cmdLine", tmpStr))
	{
		strcpy(tmp, tmpStr.c_str());
		strcat(args, " ");
		strcat(args, tmp);
	}

	// Command line args
	if (*lpCmdLine)
	{
		strcpy(tmp, lpCmdLine);
		char* dst;
		while ((dst = strstr(tmp, "--l4j-")) != NULL)
		{
			char* src = strchr(dst, ' ');
			if (src == NULL || *(src + 1) == 0)
				*dst = 0;
			else
				strcpy(dst, src + 1);
		}
		if (*tmp)
		{
			strcat(args, " ");
			strcat(args, tmp);
		}
	}

    if (debug)
	{
		strncpy(tmp, exePath, pathLen);
		*(tmp + pathLen) = 0;
		strcat(tmp, "\\launch.log");
		FILE *hFile = fopen(tmp, "w");
		if (hFile == NULL)
			return FALSE;

		fprintf(hFile, "Working dir:\t%s\n", workingDir);
		fprintf(hFile, "Launcher:\t%s\n", cmd);
        _itoa(strlen(args), tmp, 10);     // 10 -- radix
		fprintf(hFile, "Args length:\t%s/32768 chars\n", tmp);
		fprintf(hFile, "Launcher args:\t%s\n\n\n", args);
		fclose(hFile);
    }

	return TRUE;
}

void closeHandles()
{
	CloseHandle(pi.hThread);
	CloseHandle(pi.hProcess);
}

/*
 * Append a path to the Path environment variable
 */
BOOL appendToPathVar(const char* path)
{
	char chBuf[MAX_VAR_SIZE] = {0};

	const int pathSize = GetEnvironmentVariable("Path", chBuf, MAX_VAR_SIZE);
	if (MAX_VAR_SIZE - pathSize - 1 < (int)strlen(path))
		return FALSE;

	strcat(chBuf, ";");
	strcat(chBuf, path);
	return SetEnvironmentVariable("Path", chBuf);
}

DWORD execute(const BOOL wait)
{
	STARTUPINFO si;
    memset(&pi, 0, sizeof(pi));
    memset(&si, 0, sizeof(si));
    si.cb = sizeof(si);

	DWORD dwExitCode = -1;
	char cmdline[MAX_ARGS];
    strcpy(cmdline, "\"");
	strcat(cmdline, cmd);
	strcat(cmdline, "\" ");
	strcat(cmdline, args);
	if (CreateProcess(NULL, cmdline, NULL, NULL, TRUE, priority, NULL, NULL, &si, &pi))
	{
		if (wait)
		{
			WaitForSingleObject(pi.hProcess, INFINITE);
			GetExitCodeProcess(pi.hProcess, &dwExitCode);
			closeHandles();
		}
		else
			dwExitCode = 0;
	}

	return dwExitCode;
}

int APIENTRY WinMain(HINSTANCE hInstance,
                     HINSTANCE hPrevInstance,
                     LPSTR     lpCmdLine,
                     int       nCmdShow)
{
	// Read config
	if (!config.init("jar2exe.xml"))
	{
		errMsg = "jar2exe.xml appears to be missing.";
		signalError();
		return 3;
	}

	int result = prepare(lpCmdLine);
	if (result == ERROR_ALREADY_EXISTS)
	{
		HWND handle = getInstanceWindow();
		ShowWindow(handle, SW_SHOW);
		SetForegroundWindow(handle);
		return 2;
	}
	if (result != TRUE)
	{
		signalError();
		return 1;
	}

	string tmpStr;
	splash = config.getString("splash/file", tmpStr);
	splash = (tmpStr.length() > 0);
	splash = splash && strstr(lpCmdLine, "--l4j-no-splash") == NULL;

	stayAlive = config.getBoolean("stayAlive") && strstr(lpCmdLine, "--l4j-dont-wait") == NULL;
	if (splash || stayAlive)
	{
		hWnd = CreateWindowEx(WS_EX_TOOLWINDOW, "STATIC", "",
							  WS_POPUP | SS_BITMAP,
							  0, 0, CW_USEDEFAULT, CW_USEDEFAULT, NULL, NULL, hInstance, NULL);
		if (splash)
		{
			if (config.getString("splash/timeout", tmpStr))
			{
				splashTimeout = atoi(tmpStr.c_str());
				if (splashTimeout <= 0 || splashTimeout > MAX_SPLASH_TIMEOUT) {
					splashTimeout = DEFAULT_SPLASH_TIMEOUT;
				}
			}
			splashTimeoutErr = config.getBoolean("splash/timeoutErr") && strstr(lpCmdLine, "--l4j-no-splash-err") == NULL;
			waitForWindow = config.getBoolean("splash/waitForWindow");
			HANDLE hImage = LoadImage(hInstance,						// handle of the instance containing the image
									  MAKEINTRESOURCE(SPLASH_BITMAP),	// name or identifier of image
									  IMAGE_BITMAP,					// type of image
									  0,								// desired width
									  0,								// desired height
									  LR_DEFAULTSIZE);
			if (hImage == NULL)
			{
				signalError();
				return 1;
			}
			SendMessage(hWnd, STM_SETIMAGE, IMAGE_BITMAP, (LPARAM) hImage);
			RECT rect;
			GetWindowRect(hWnd, &rect);
			int x = (GetSystemMetrics(SM_CXSCREEN) - (rect.right - rect.left)) / 2;
			int y = (GetSystemMetrics(SM_CYSCREEN) - (rect.bottom - rect.top)) / 2;
			SetWindowPos(hWnd, HWND_TOP, x, y, 0, 0, SWP_NOSIZE);
			ShowWindow(hWnd, nCmdShow);
			UpdateWindow (hWnd);
		}
		if (!SetTimer (hWnd, ID_TIMER, 1000 /* 1s */, TimerProc))
			return 1;

	}
	if (execute(FALSE) == -1)
	{
		signalError();
		return 1;
	}
	if (!(splash || stayAlive))
	{
		closeHandles();
		return 0;
	}
	MSG msg;
	while (GetMessage(&msg, NULL, 0, 0))
	{
		TranslateMessage(&msg);
		DispatchMessage(&msg);
	}
	closeHandles();

	return dwExitCode;
}

HWND getInstanceWindow()
{
	char windowTitle[STR];
	char instWindowTitle[STR] = {0};
	string tmpStr;
	if (config.getString("singleInstance/windowTitle", tmpStr))
	{
		strcpy(instWindowTitle, tmpStr.c_str());
		HWND handle = FindWindowEx(NULL, NULL, NULL, NULL); 
		while (handle != NULL)
		{
			GetWindowText(handle, windowTitle, STR - 1);
			if (strstr(windowTitle, instWindowTitle) != NULL)
				return handle;
			else
				handle = FindWindowEx(NULL, handle, NULL, NULL);
		}
	}

	return NULL;   
}

BOOL CALLBACK enumwndfn(HWND hwnd, LPARAM lParam)
{
	DWORD processId;
	GetWindowThreadProcessId(hwnd, &processId);
	if (pi.dwProcessId == processId)
	{
		LONG styles = GetWindowLong(hwnd, GWL_STYLE);
		if ((styles & WS_VISIBLE) != 0)
		{
			splash = FALSE;
			ShowWindow(hWnd, SW_HIDE);
			return FALSE;
		}
	}

	return TRUE;
}

VOID CALLBACK TimerProc
(
	HWND hwnd,			// handle of window for timer messages
	UINT uMsg,			// WM_TIMER message
	UINT idEvent,		// timer identifier
	DWORD dwTime) {		// current system time
	
	if (splash)
	{
		if (splashTimeout == 0)
		{
			splash = FALSE;
			ShowWindow(hWnd, SW_HIDE);
			if (waitForWindow && splashTimeoutErr)
			{
				KillTimer(hwnd, ID_TIMER);
				signalError();
				PostQuitMessage(0);
			}
		}
		else
		{
			splashTimeout--;
			if (waitForWindow)
				EnumWindows(enumwndfn, 0);
		}
	}
	GetExitCodeProcess(pi.hProcess, &dwExitCode);
	if ((dwExitCode != STILL_ACTIVE) || !(splash || stayAlive))
	{
		KillTimer(hWnd, ID_TIMER);
		PostQuitMessage(0);	
		return;
	}
}

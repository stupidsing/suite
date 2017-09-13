// gcc -mwindows src/main/c/kbhook.c -o target/kbhook
#include <stdio.h>
#include <windows.h>

LRESULT CALLBACK LowLevelKeyboardProc(int nCode, WPARAM wParam, LPARAM lParam) {
	KBDLLHOOKSTRUCT *pKeyboard = (KBDLLHOOKSTRUCT*) lParam;
	char pressedKey;
	switch(wParam) {
	case WM_KEYDOWN:
		pressedKey = (char) pKeyboard->vkCode;
		printf("DN %d\n", pressedKey);
		break;
	case WM_KEYUP:
		pressedKey = (char) pKeyboard->vkCode;
		printf("UP %d\n", pressedKey);
		break;
	default:
		return CallNextHookEx(NULL, nCode, wParam, lParam);
	}
	return CallNextHookEx(NULL, nCode, wParam, lParam);
}

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR szCmdLine, int iCmdShow) {
	HHOOK hook = SetWindowsHookEx(WH_KEYBOARD_LL, LowLevelKeyboardProc, GetModuleHandle(NULL), 0);
	MSG msg;

	while(GetMessage(&msg, NULL, 0, 0)) {
		TranslateMessage(&msg); /* for certain keyboard messages */
		DispatchMessage(&msg); /* send message to WndProc */
	}

	return msg.wParam;
}

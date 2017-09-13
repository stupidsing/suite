// gcc -mwindows src/main/c/kbhook.c -o target/kbhook
#include <stdio.h>
#include <windows.h>

int isMod = 0;
int rightCtrl = 0;
int rightAlt = 0;

LRESULT CALLBACK LowLevelKeyboardProc(int nCode, WPARAM wParam, LPARAM lParam) {
	KBDLLHOOKSTRUCT *pKeyboard = (KBDLLHOOKSTRUCT*) lParam;
	char pressedKey;
	switch(wParam) {
	case WM_KEYDOWN:
		pressedKey = (char) pKeyboard->vkCode;
		printf("DN %d\n", pressedKey);
		if(isMod)
			isMod = 0;
		else if(rightCtrl) {
			INPUT input;
			input.type = INPUT_KEYBOARD;
			input.ki.dwExtraInfo = 0;
			input.ki.dwFlags = KEYEVENTF_KEYUP;
			input.ki.wScan = 0;
			input.ki.wVk = VK_RCONTROL;
			input.ki.time = 0;
			SendInput(1, &input, sizeof(INPUT));
			rightCtrl = 0;
		} else if(rightAlt) {
			INPUT input;
			input.type = INPUT_KEYBOARD;
			input.ki.dwExtraInfo = 0;
			input.ki.dwFlags = KEYEVENTF_KEYUP;
			input.ki.wScan = 0;
			input.ki.wVk = VK_MENU;
			input.ki.time = 0;
			SendInput(1, &input, sizeof(INPUT));
			rightAlt = 0;
		}
		break;
	case WM_KEYUP:
		pressedKey = (char) pKeyboard->vkCode;
		printf("UP %d\n", pressedKey);
		if(pressedKey == -23) {
			INPUT input;
			input.type = INPUT_KEYBOARD;
			input.ki.dwExtraInfo = 0;
			input.ki.dwFlags = 0;
			input.ki.wScan = 0;
			input.ki.wVk = VK_RCONTROL;
			input.ki.time = 0;
			SendInput(1, &input, sizeof(INPUT));
			isMod = rightCtrl = 1;
		} else if(pressedKey == -1) {
			INPUT input;
			input.type = INPUT_KEYBOARD;
			input.ki.dwExtraInfo = 0;
			input.ki.dwFlags = 0;
			input.ki.wScan = 0;
			input.ki.wVk = VK_MENU;
			input.ki.time = 0;
			SendInput(1, &input, sizeof(INPUT));
			isMod = rightAlt = 1;
		}
		break;
	default:
		printf("UNK %d %d %d\n", nCode, wParam, lParam);
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

	UnhookWindowsHookEx(hook);
	return msg.wParam;
}

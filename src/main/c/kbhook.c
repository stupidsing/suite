// taskkill /f /im kbhook.exe; gcc -mwindows src/main/c/kbhook.c -o target/kbhook && target/kbhook.exe > /dev/null &
#include <stdio.h>
#include <windows.h>

int rightCtrl = 0;
int rightAlt = 0;

void send(int dwf, int vk) {
	INPUT input;
	input.type = INPUT_KEYBOARD;
	input.ki.dwExtraInfo = 0;
	input.ki.dwFlags = dwf;
	input.ki.wScan = 0;
	input.ki.wVk = vk;
	input.ki.time = 0;
	SendInput(1, &input, sizeof(INPUT));
	rightCtrl = 0;
}

void restore() {
	if(rightCtrl) {
		send(KEYEVENTF_KEYUP, VK_RCONTROL);
		rightCtrl = 0;
	} else if(rightAlt) {
		send(KEYEVENTF_KEYUP, VK_MENU);
		rightAlt = 0;
	}
}

LRESULT CALLBACK LowLevelKeyboardProc(int nCode, WPARAM wParam, LPARAM lParam) {
	KBDLLHOOKSTRUCT *pKeyboard = (KBDLLHOOKSTRUCT*) lParam;
	char vk;
	switch(wParam) {
	case WM_KEYDOWN:
		vk = (char) pKeyboard->vkCode;
		printf("DN %d\n", vk);
		break;
	case WM_KEYUP:
		vk = (char) pKeyboard->vkCode;
		printf("UP %d\n", vk);
		if(vk == -23) {
			send(0, VK_RCONTROL);
			rightCtrl = 1;
		} else if(vk == -1) {
			send(0, VK_MENU);
			rightAlt = 1;
		} else if(vk != VK_NEXT && vk != VK_PRIOR)
			restore();
		break;
	// case WM_SYSKEYDOWN: break;
	// case WM_SYSKEYUP: break;
	default: printf("UNK %d %d %d\n", nCode, wParam, lParam);
	}
	return CallNextHookEx(NULL, nCode, wParam, lParam);
}

LRESULT CALLBACK LowLevelMouseProc(int nCode, WPARAM wParam, LPARAM lParam) {
	MSLLHOOKSTRUCT *pMouse = (MSLLHOOKSTRUCT*) lParam;
	switch(wParam) {
    case WM_LBUTTONDOWN: restore(); break;
    // case WM_LBUTTONUP: break;
    // case WM_MOUSEHWHEEL: break;
    // case WM_MOUSEMOVE: break;
    // case WM_MOUSEWHEEL: break;
    case WM_RBUTTONDOWN: restore(); break;
    // case WM_RBUTTONUP: break;
    default: printf("UNK %d %d %d\n", nCode, wParam, lParam);
	}
	return CallNextHookEx(NULL, nCode, wParam, lParam);
}

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR szCmdLine, int iCmdShow) {
	HANDLE mod = GetModuleHandle(NULL);
	HHOOK hkb = SetWindowsHookEx(WH_KEYBOARD_LL, LowLevelKeyboardProc, mod, 0);
	HHOOK hms = SetWindowsHookEx(WH_MOUSE_LL, LowLevelMouseProc, mod, 0);
	MSG msg;

	while(GetMessage(&msg, NULL, 0, 0)) {
		TranslateMessage(&msg);
		DispatchMessage(&msg);
	}

	UnhookWindowsHookEx(hms);
	UnhookWindowsHookEx(hkb);
	return msg.wParam;
}

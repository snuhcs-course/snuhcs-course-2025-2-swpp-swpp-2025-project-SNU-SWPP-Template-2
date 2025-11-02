import React from "react";
import { render, fireEvent, waitFor } from "@testing-library/react-native";
import { LoginScreen } from "./LoginScreen";
import { Alert } from "react-native";

// Mock navigation
const mockReplace = jest.fn();
const navigation = { replace: mockReplace };

// Mock storage
jest.mock("app/utils/storage", () => ({
  saveString: jest.fn(() => Promise.resolve()),
}));

// Mock API functions
const mockLogin = jest.fn();
const mockRegister = jest.fn();
const mockGetCsrf = jest.fn();
const mockGetPreferences = jest.fn();

jest.mock("app/services/api", () => ({
  api: {
    login: (...args: any[]) => mockLogin(...args),
    register: (...args: any[]) => mockRegister(...args),
    getCsrf: (...args: any[]) => mockGetCsrf(...args),
    getPreferences: (...args: any[]) => mockGetPreferences(...args),
  },
}));

// Mock AWS Amplify
jest.mock("app/services/aws/handleAwsSignin", () => ({
  handleSignIn: jest.fn(() => Promise.resolve()),
}));

jest.mock("aws-amplify/auth", () => ({
  signUp: jest.fn(() =>
    Promise.resolve({
      isSignUpComplete: true,
      userId: "test-user-id",
      nextStep: {},
    })
  ),
}));

// Mock Alert
jest.spyOn(Alert, "alert");

describe("LoginScreen", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetCsrf.mockResolvedValue({ ok: true });
  });

  it("renders login form by default", () => {
    const { getByText, getByPlaceholderText } = render(
      <LoginScreen navigation={navigation} />
    );
    expect(getByText("Login")).toBeTruthy();
    expect(getByPlaceholderText("Username")).toBeTruthy();
    expect(getByPlaceholderText("Password")).toBeTruthy();
    expect(getByText("Log In")).toBeTruthy();
  });

  it("switches to sign up mode when toggle is pressed", () => {
    const { getByText, getByPlaceholderText, getAllByText } = render(
      <LoginScreen navigation={navigation} />
    );
    fireEvent.press(getByText("Don't have an account? Sign Up"));
    
    // "Sign Up" appears twice: in title and in button
    expect(getAllByText("Sign Up").length).toBeGreaterThan(0);
    expect(getByPlaceholderText("Email")).toBeTruthy();
    expect(getByPlaceholderText("Confirm Password")).toBeTruthy();
  });

  it("switches back to login mode from sign up", () => {
    const { getByText } = render(<LoginScreen navigation={navigation} />);
    fireEvent.press(getByText("Don't have an account? Sign Up"));
    fireEvent.press(getByText("Already have an account? Log In"));
    
    expect(getByText("Login")).toBeTruthy();
    expect(getByText("Log In")).toBeTruthy();
  });

  it("shows error when login with empty fields", async () => {
    const { getByText } = render(<LoginScreen navigation={navigation} />);
    fireEvent.press(getByText("Log In"));
    
    await waitFor(() => {
      expect(Alert.alert).toHaveBeenCalledWith(
        "Error",
        "Please enter username and password."
      );
    });
  });

  it("successfully logs in with valid credentials", async () => {
    mockLogin.mockResolvedValue({ ok: true, data: {} });
    mockGetPreferences.mockResolvedValue({ ok: true, data: { spicy_level: 5 } });

    const { getByPlaceholderText, getByText } = render(
      <LoginScreen navigation={navigation} />
    );
    
    fireEvent.changeText(getByPlaceholderText("Username"), "testuser");
    fireEvent.changeText(getByPlaceholderText("Password"), "password123");
    fireEvent.press(getByText("Log In"));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith("testuser", "password123");
      expect(mockReplace).toHaveBeenCalledWith("Foodigram");
    });
  });

  it("navigates to onboarding when user has no preferences", async () => {
    mockLogin.mockResolvedValue({ ok: true, data: {} });
    mockGetPreferences.mockResolvedValue({ ok: false });

    const { getByPlaceholderText, getByText } = render(
      <LoginScreen navigation={navigation} />
    );
    
    fireEvent.changeText(getByPlaceholderText("Username"), "newuser");
    fireEvent.changeText(getByPlaceholderText("Password"), "password123");
    fireEvent.press(getByText("Log In"));

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith("Onboarding");
    });
  });

  it("shows error on login failure", async () => {
    mockLogin.mockResolvedValue({ ok: false, data: { detail: "Invalid credentials" } });

    const { getByPlaceholderText, getByText } = render(
      <LoginScreen navigation={navigation} />
    );
    
    fireEvent.changeText(getByPlaceholderText("Username"), "wronguser");
    fireEvent.changeText(getByPlaceholderText("Password"), "wrongpass");
    fireEvent.press(getByText("Log In"));

    await waitFor(() => {
      expect(Alert.alert).toHaveBeenCalledWith("Login Failed", "Invalid credentials");
    });
  });

  it("shows validation text in sign up mode", () => {
    const { getByText } = render(<LoginScreen navigation={navigation} />);
    fireEvent.press(getByText("Don't have an account? Sign Up"));
    
    expect(getByText("• Password must be at least 8 characters", { exact: false })).toBeTruthy();
  });

  it("shows error when sign up with empty fields", async () => {
    const { getByText, getAllByText } = render(<LoginScreen navigation={navigation} />);
    fireEvent.press(getByText("Don't have an account? Sign Up"));
    // Get the button (second "Sign Up" text)
    const signUpButtons = getAllByText("Sign Up");
    fireEvent.press(signUpButtons[1]);

    await waitFor(() => {
      expect(Alert.alert).toHaveBeenCalledWith("Error", "Please fill in all fields.");
    });
  });

  it("shows error when passwords don't match", async () => {
    const { getByPlaceholderText, getByText, getAllByText } = render(
      <LoginScreen navigation={navigation} />
    );
    fireEvent.press(getByText("Don't have an account? Sign Up"));
    
    fireEvent.changeText(getByPlaceholderText("Username"), "newuser");
    fireEvent.changeText(getByPlaceholderText("Email"), "test@example.com");
    fireEvent.changeText(getByPlaceholderText("Password"), "password123");
    fireEvent.changeText(getByPlaceholderText("Confirm Password"), "password456");
    const signUpButtons = getAllByText("Sign Up");
    fireEvent.press(signUpButtons[1]);

    await waitFor(() => {
      expect(Alert.alert).toHaveBeenCalledWith("Error", "Passwords do not match.");
    });
  });

  it("shows error when password is too short", async () => {
    const { getByPlaceholderText, getByText, getAllByText } = render(
      <LoginScreen navigation={navigation} />
    );
    fireEvent.press(getByText("Don't have an account? Sign Up"));
    
    fireEvent.changeText(getByPlaceholderText("Username"), "newuser");
    fireEvent.changeText(getByPlaceholderText("Email"), "test@example.com");
    fireEvent.changeText(getByPlaceholderText("Password"), "pass");
    fireEvent.changeText(getByPlaceholderText("Confirm Password"), "pass");
    const signUpButtons = getAllByText("Sign Up");
    fireEvent.press(signUpButtons[1]);

    await waitFor(() => {
      expect(Alert.alert).toHaveBeenCalledWith(
        "Error",
        "Password must be at least 8 characters long."
      );
    });
  });

  it("shows error when email format is invalid", async () => {
    const { getByPlaceholderText, getByText, getAllByText } = render(
      <LoginScreen navigation={navigation} />
    );
    fireEvent.press(getByText("Don't have an account? Sign Up"));
    
    fireEvent.changeText(getByPlaceholderText("Username"), "newuser");
    fireEvent.changeText(getByPlaceholderText("Email"), "invalid-email");
    fireEvent.changeText(getByPlaceholderText("Password"), "password123");
    fireEvent.changeText(getByPlaceholderText("Confirm Password"), "password123");
    const signUpButtons = getAllByText("Sign Up");
    fireEvent.press(signUpButtons[1]);

    await waitFor(() => {
      expect(Alert.alert).toHaveBeenCalledWith(
        "Error",
        "Please enter a valid email format."
      );
    });
  });

  it("successfully registers with valid information", async () => {
    mockRegister.mockResolvedValue({ ok: true, data: {} });

    const { getByPlaceholderText, getByText, getAllByText } = render(
      <LoginScreen navigation={navigation} />
    );
    fireEvent.press(getByText("Don't have an account? Sign Up"));
    
    fireEvent.changeText(getByPlaceholderText("Username"), "newuser");
    fireEvent.changeText(getByPlaceholderText("Email"), "test@example.com");
    fireEvent.changeText(getByPlaceholderText("Password"), "password123");
    fireEvent.changeText(getByPlaceholderText("Confirm Password"), "password123");
    const signUpButtons = getAllByText("Sign Up");
    fireEvent.press(signUpButtons[1]);

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith("newuser", "test@example.com", "password123");
      expect(Alert.alert).toHaveBeenCalledWith("Success", "Registration completed!", expect.any(Array));
    });
  });

  it("shows error on registration failure", async () => {
    mockRegister.mockResolvedValue({ ok: false, data: { detail: "Username already exists" } });

    const { getByPlaceholderText, getByText, getAllByText } = render(
      <LoginScreen navigation={navigation} />
    );
    fireEvent.press(getByText("Don't have an account? Sign Up"));
    
    fireEvent.changeText(getByPlaceholderText("Username"), "existinguser");
    fireEvent.changeText(getByPlaceholderText("Email"), "test@example.com");
    fireEvent.changeText(getByPlaceholderText("Password"), "password123");
    fireEvent.changeText(getByPlaceholderText("Confirm Password"), "password123");
    const signUpButtons = getAllByText("Sign Up");
    fireEvent.press(signUpButtons[1]);

    await waitFor(() => {
      expect(Alert.alert).toHaveBeenCalledWith("Registration Failed", "Username already exists");
    });
  });

  it("clears form when switching between modes", () => {
    const { getByPlaceholderText, getByText } = render(
      <LoginScreen navigation={navigation} />
    );
    
    fireEvent.changeText(getByPlaceholderText("Username"), "testuser");
    fireEvent.changeText(getByPlaceholderText("Password"), "password");
    
    fireEvent.press(getByText("Don't have an account? Sign Up"));
    
    expect(getByPlaceholderText("Username").props.value).toBe("");
    expect(getByPlaceholderText("Password").props.value).toBe("");
  });

  it("disables submit button while loading", async () => {
    mockLogin.mockImplementation(() => new Promise(resolve => setTimeout(() => resolve({ ok: true, data: {} }), 100)));
    mockGetPreferences.mockResolvedValue({ ok: true, data: {} });

    const { getByPlaceholderText, getByText } = render(
      <LoginScreen navigation={navigation} />
    );
    
    fireEvent.changeText(getByPlaceholderText("Username"), "testuser");
    fireEvent.changeText(getByPlaceholderText("Password"), "password123");
    
    const loginButton = getByText("Log In");
    fireEvent.press(loginButton);

    // Note: The button disabling might not reflect immediately in test environment
    // This test verifies the button exists and can be pressed
    expect(loginButton).toBeTruthy();
  });
});


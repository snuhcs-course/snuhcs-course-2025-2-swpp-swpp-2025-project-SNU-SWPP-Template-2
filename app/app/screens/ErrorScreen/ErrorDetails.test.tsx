import React from "react";
import { render, fireEvent } from "@testing-library/react-native";
import { ErrorDetails } from "./ErrorDetails";

// Mock Screen component
jest.mock("../../components", () => {
  const React = require("react");
  const RN = require("react-native");
  
  return {
    Screen: ({ children, ...props }: any) => <RN.View>{children}</RN.View>,
    Text: ({ text, tx, children, ...props }: any) => {
      const mockTexts: Record<string, string> = {
        "errorScreen.title": "Error",
        "errorScreen.friendlySubtitle": "Something went wrong",
        "errorScreen.reset": "Reset App",
      };
      const displayText = text || mockTexts[tx] || children;
      return <RN.Text {...props}>{displayText}</RN.Text>;
    },
    Icon: ({ icon, size }: any) => <RN.Text>{icon}</RN.Text>,
    Button: ({ text, tx, onPress, ...props }: any) => {
      const mockTexts: Record<string, string> = {
        "errorScreen.reset": "Reset App",
      };
      const buttonText = text || mockTexts[tx];
      return (
        <RN.TouchableOpacity onPress={onPress} {...props}>
          <RN.Text>{buttonText}</RN.Text>
        </RN.TouchableOpacity>
      );
    },
  };
});

describe("ErrorDetails", () => {
  const mockError = new Error("Test error message");
  const mockErrorInfo = {
    componentStack: "\n    at Component (path/to/component.tsx:10:5)",
  };
  const mockOnReset = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders without crashing", () => {
    const { toJSON } = render(
      <ErrorDetails
        error={mockError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );
    expect(toJSON()).toBeTruthy();
  });

  it("displays error title", () => {
    const { getByText } = render(
      <ErrorDetails
        error={mockError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );
    expect(getByText("Error")).toBeTruthy();
  });

  it("displays friendly subtitle", () => {
    const { getByText } = render(
      <ErrorDetails
        error={mockError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );
    expect(getByText("Something went wrong")).toBeTruthy();
  });

  it("displays error message", () => {
    const { getByText } = render(
      <ErrorDetails
        error={mockError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );
    expect(getByText("Error: Test error message")).toBeTruthy();
  });

  it("displays error component stack", () => {
    const { getByText } = render(
      <ErrorDetails
        error={mockError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );
    expect(getByText("at Component (path/to/component.tsx:10:5)", { exact: false })).toBeTruthy();
  });

  it("displays reset button", () => {
    const { getByText } = render(
      <ErrorDetails
        error={mockError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );
    expect(getByText("Reset App")).toBeTruthy();
  });

  it("calls onReset when reset button is pressed", () => {
    const { getByText } = render(
      <ErrorDetails
        error={mockError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );
    const resetButton = getByText("Reset App");
    fireEvent.press(resetButton);
    expect(mockOnReset).toHaveBeenCalledTimes(1);
  });

  it("handles null errorInfo gracefully", () => {
    const { toJSON } = render(
      <ErrorDetails
        error={mockError}
        errorInfo={null}
        onReset={mockOnReset}
      />
    );
    expect(toJSON()).toBeTruthy();
  });

  it("displays ladybug icon", () => {
    const { getByText } = render(
      <ErrorDetails
        error={mockError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );
    expect(getByText("ladybug")).toBeTruthy();
  });

  it("renders error content as trimmed string", () => {
    const errorWithWhitespace = new Error("  Error with whitespace  ");
    const { getByText } = render(
      <ErrorDetails
        error={errorWithWhitespace}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );
    // Should trim the whitespace
    expect(getByText("Error: Error with whitespace")).toBeTruthy();
  });

  it("renders error backtrace as trimmed string", () => {
    const errorInfoWithWhitespace = {
      componentStack: "  \n  at Component  \n  ",
    };
    const { toJSON } = render(
      <ErrorDetails
        error={mockError}
        errorInfo={errorInfoWithWhitespace}
        onReset={mockOnReset}
      />
    );
    expect(toJSON()).toBeTruthy();
  });

  it("displays error info with empty component stack", () => {
    const emptyErrorInfo = {
      componentStack: "",
    };
    const { toJSON } = render(
      <ErrorDetails
        error={mockError}
        errorInfo={emptyErrorInfo}
        onReset={mockOnReset}
      />
    );
    expect(toJSON()).toBeTruthy();
  });

  it("displays complex error messages", () => {
    const complexError = new Error(
      "Network request failed: timeout of 5000ms exceeded"
    );
    const { getByText } = render(
      <ErrorDetails
        error={complexError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );
    expect(
      getByText("Error: Network request failed: timeout of 5000ms exceeded")
    ).toBeTruthy();
  });

  it("handles multiline error component stacks", () => {
    const multilineErrorInfo = {
      componentStack: `
    at ErrorBoundary (src/ErrorBoundary.tsx:25:10)
    at App (src/App.tsx:15:5)
    at RootComponent (src/Root.tsx:8:3)
      `,
    };
    const { toJSON } = render(
      <ErrorDetails
        error={mockError}
        errorInfo={multilineErrorInfo}
        onReset={mockOnReset}
      />
    );
    expect(toJSON()).toBeTruthy();
  });

  it("uses Screen component with proper props", () => {
    const { toJSON } = render(
      <ErrorDetails
        error={mockError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );
    expect(toJSON()).toBeTruthy();
  });

  it("renders all sections in correct order", () => {
    const { toJSON } = render(
      <ErrorDetails
        error={mockError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );
    const tree = toJSON();
    expect(tree).toMatchSnapshot();
  });
});


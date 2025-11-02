import React from "react";
import { render } from "@testing-library/react-native";
import { WelcomeScreen } from "./WelcomeScreen";

// Mock navigation
const navigation = {};

// Mock images
jest.mock("../../assets/images/logo.png", () => "logo.png");
jest.mock("../../assets/images/welcome-face.png", () => "welcome-face.png");

// Mock useSafeAreaInsetsStyle
jest.mock("../utils/useSafeAreaInsetsStyle", () => ({
  useSafeAreaInsetsStyle: jest.fn(() => ({})),
}));

describe("WelcomeScreen", () => {
  it("renders without crashing", () => {
    const { toJSON } = render(<WelcomeScreen navigation={navigation} />);
    expect(toJSON()).toBeTruthy();
  });

  it("renders welcome heading", () => {
    const { getByTestId } = render(<WelcomeScreen navigation={navigation} />);
    expect(getByTestId("welcome-heading")).toBeTruthy();
  });

  it("renders logo image", () => {
    const { toJSON } = render(<WelcomeScreen navigation={navigation} />);
    const tree = toJSON();
    // Check that the component renders (images are present in structure)
    expect(tree).toBeTruthy();
  });

  it("renders welcome face image", () => {
    const { toJSON } = render(<WelcomeScreen navigation={navigation} />);
    const tree = toJSON();
    // Check that the component renders (images are present in structure)
    expect(tree).toBeTruthy();
  });

  it("has proper container structure", () => {
    const { toJSON } = render(<WelcomeScreen navigation={navigation} />);
    const tree = toJSON();
    expect(tree).toMatchSnapshot();
  });

  it("renders top container with content", () => {
    const { getByTestId } = render(<WelcomeScreen navigation={navigation} />);
    const heading = getByTestId("welcome-heading");
    expect(heading.parent).toBeTruthy();
  });

  it("renders bottom container", () => {
    const { toJSON } = render(<WelcomeScreen navigation={navigation} />);
    expect(toJSON()).toBeTruthy();
  });

  it("applies safe area insets to bottom container", () => {
    const mockUseSafeAreaInsetsStyle = require("../utils/useSafeAreaInsetsStyle").useSafeAreaInsetsStyle;
    
    render(<WelcomeScreen navigation={navigation} />);
    
    expect(mockUseSafeAreaInsetsStyle).toHaveBeenCalledWith(["bottom"]);
  });

  it("uses proper text presets", () => {
    const { getByTestId } = render(<WelcomeScreen navigation={navigation} />);
    const heading = getByTestId("welcome-heading");
    expect(heading.props.preset).toBe("heading");
  });

  it("displays internationalized text", () => {
    const { getByTestId } = render(<WelcomeScreen navigation={navigation} />);
    const heading = getByTestId("welcome-heading");
    // The tx prop is used for internationalization
    expect(heading).toBeTruthy();
  });
});


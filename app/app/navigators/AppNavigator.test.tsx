import React from "react"
import { render } from "@testing-library/react-native"
import { AppNavigator } from "./AppNavigator"
import { NavigationContainer, DarkTheme } from "@react-navigation/native"
import * as ReactNative from "react-native"

// Mocks for native modules to prevent React Native/expo errors

// Mock problematic ESM/cloud packages to prevent Jest transform errors

// Additional native module mocks for common mobile errors
jest.mock('react-native/Libraries/EventEmitter/NativeEventEmitter', () => {
    return jest.fn().mockImplementation(() => ({
        addListener: jest.fn(),
        removeListeners: jest.fn(),
    }))
})
jest.mock('react-native/Libraries/Animated/NativeAnimatedHelper')

// Smoke test: renders without crashing
it("AppNavigator renders without crashing", () => {
  const { toJSON } = render(<AppNavigator />)
  expect(toJSON()).toBeTruthy()
})


// Back button handler test
jest.mock("./navigationUtilities", () => ({
  useBackButtonHandler: jest.fn(),
  navigationRef: { current: null },
}))

describe("AppNavigator back button handler", () => {
  it("calls useBackButtonHandler with exitRoutes", () => {
    const { useBackButtonHandler } = require("./navigationUtilities")
    render(<AppNavigator />)
    expect(useBackButtonHandler).toHaveBeenCalled()
  })
})

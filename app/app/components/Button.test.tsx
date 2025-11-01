import { render, fireEvent } from "@testing-library/react-native"
import React from "react"
import { Button } from "./Button"

describe("Button", () => {
  it("renders with text", () => {
    const { getByText } = render(<Button text="Click me" />)
    expect(getByText("Click me")).toBeTruthy()
  })

  it("calls onPress when pressed", () => {
    const onPressMock = jest.fn()
    const { getByRole } = render(<Button text="Press" onPress={onPressMock} />)
    fireEvent.press(getByRole("button"))
    expect(onPressMock).toHaveBeenCalledTimes(1)
  })

  it("renders with default preset", () => {
    const { toJSON } = render(<Button text="Default" />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders with filled preset", () => {
    const { toJSON } = render(<Button text="Filled" preset="filled" />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders with reversed preset", () => {
    const { toJSON } = render(<Button text="Reversed" preset="reversed" />)
    expect(toJSON()).toBeTruthy()
  })

  it("applies custom style", () => {
    const { toJSON } = render(<Button text="Styled" style={{ backgroundColor: "red" }} />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders when disabled", () => {
    const { getByRole } = render(<Button text="Disabled" disabled />)
    const button = getByRole("button")
    expect(button.props.accessibilityState.disabled).toBe(true)
  })

  it("does not call onPress when disabled", () => {
    const onPressMock = jest.fn()
    const { getByRole } = render(<Button text="Disabled" disabled onPress={onPressMock} />)
    fireEvent.press(getByRole("button"))
    expect(onPressMock).not.toHaveBeenCalled()
  })
})


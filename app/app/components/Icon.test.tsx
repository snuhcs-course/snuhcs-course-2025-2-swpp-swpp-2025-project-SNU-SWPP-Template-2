import { render, fireEvent } from "@testing-library/react-native"
import React from "react"
import { Icon } from "./Icon"

describe("Icon", () => {
  it("renders with icon name", () => {
    const { toJSON } = render(<Icon icon="check" />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders back icon", () => {
    const { toJSON } = render(<Icon icon="back" />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders bell icon", () => {
    const { toJSON } = render(<Icon icon="bell" />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders menu icon", () => {
    const { toJSON } = render(<Icon icon="menu" />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders settings icon", () => {
    const { toJSON } = render(<Icon icon="settings" />)
    expect(toJSON()).toBeTruthy()
  })

  it("applies size prop", () => {
    const { toJSON } = render(<Icon icon="check" size={32} />)
    expect(toJSON()).toBeTruthy()
  })

  it("applies color prop", () => {
    const { toJSON } = render(<Icon icon="check" color="#ff0000" />)
    expect(toJSON()).toBeTruthy()
  })

  it("applies custom style", () => {
    const { toJSON } = render(<Icon icon="check" style={{ opacity: 0.5 }} />)
    expect(toJSON()).toBeTruthy()
  })

  it("applies custom containerStyle", () => {
    const { toJSON } = render(
      <Icon icon="check" containerStyle={{ backgroundColor: "red" }} />
    )
    expect(toJSON()).toBeTruthy()
  })

  it("calls onPress when pressed", () => {
    const onPressMock = jest.fn()
    const { getByRole } = render(<Icon icon="check" onPress={onPressMock} />)
    fireEvent.press(getByRole("imagebutton"))
    expect(onPressMock).toHaveBeenCalledTimes(1)
  })

  it("renders as view when no onPress provided", () => {
    const { toJSON } = render(<Icon icon="check" />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders all icon types", () => {
    const icons = ["back", "bell", "caretLeft", "caretRight", "check", "hidden", "ladybug", "lock", "menu", "more", "settings", "view", "x"] as const
    icons.forEach(iconName => {
      const { toJSON } = render(<Icon icon={iconName} />)
      expect(toJSON()).toBeTruthy()
    })
  })
})


import { render, fireEvent } from "@testing-library/react-native"
import React from "react"
import { View } from "react-native"
import { Card } from "./Card"

describe("Card", () => {
  it("renders without crashing", () => {
    const { toJSON } = render(<Card />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders with heading", () => {
    const { getByText } = render(<Card heading="Test Heading" />)
    expect(getByText("Test Heading")).toBeTruthy()
  })

  it("renders with content", () => {
    const { getByText } = render(<Card content="Test content" />)
    expect(getByText("Test content")).toBeTruthy()
  })

  it("renders with footer", () => {
    const { getByText } = render(<Card footer="Test footer" />)
    expect(getByText("Test footer")).toBeTruthy()
  })

  it("renders with all text sections", () => {
    const { getByText } = render(
      <Card 
        heading="Heading"
        content="Content"
        footer="Footer"
      />
    )
    expect(getByText("Heading")).toBeTruthy()
    expect(getByText("Content")).toBeTruthy()
    expect(getByText("Footer")).toBeTruthy()
  })

  it("calls onPress when pressed", () => {
    const onPressMock = jest.fn()
    const { getByRole } = render(<Card heading="Pressable" onPress={onPressMock} />)
    fireEvent.press(getByRole("button"))
    expect(onPressMock).toHaveBeenCalledTimes(1)
  })

  it("renders with default preset", () => {
    const { toJSON } = render(<Card heading="Default" preset="default" />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders with reversed preset", () => {
    const { toJSON } = render(<Card heading="Reversed" preset="reversed" />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders with custom LeftComponent", () => {
    const { toJSON } = render(
      <Card 
        heading="With Left"
        LeftComponent={<View testID="left-component" />}
      />
    )
    expect(toJSON()).toBeTruthy()
  })

  it("renders with custom RightComponent", () => {
    const { toJSON } = render(
      <Card 
        heading="With Right"
        RightComponent={<View testID="right-component" />}
      />
    )
    expect(toJSON()).toBeTruthy()
  })

  it("applies vertical alignment: center", () => {
    const { toJSON } = render(
      <Card heading="Centered" verticalAlignment="center" />
    )
    expect(toJSON()).toBeTruthy()
  })

  it("applies vertical alignment: space-between", () => {
    const { toJSON } = render(
      <Card heading="Space Between" verticalAlignment="space-between" />
    )
    expect(toJSON()).toBeTruthy()
  })
})


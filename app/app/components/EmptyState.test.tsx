import { render, fireEvent } from "@testing-library/react-native"
import React from "react"
import { EmptyState } from "./EmptyState"

describe("EmptyState", () => {
  it("renders with generic preset", () => {
    const { toJSON } = render(<EmptyState preset="generic" />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders custom heading", () => {
    const { getByText } = render(<EmptyState heading="No items found" />)
    expect(getByText("No items found")).toBeTruthy()
  })

  it("renders custom content", () => {
    const { getByText } = render(<EmptyState content="Try adding some items" />)
    expect(getByText("Try adding some items")).toBeTruthy()
  })

  it("renders custom button text", () => {
    const { getByText } = render(<EmptyState button="Add Item" />)
    expect(getByText("Add Item")).toBeTruthy()
  })

  it("calls buttonOnPress when button is pressed", () => {
    const onPressMock = jest.fn()
    const { getByText } = render(
      <EmptyState button="Add" buttonOnPress={onPressMock} />
    )
    fireEvent.press(getByText("Add"))
    expect(onPressMock).toHaveBeenCalledTimes(1)
  })

  it("renders all sections together", () => {
    const { getByText } = render(
      <EmptyState 
        heading="Empty"
        content="No data"
        button="Reload"
      />
    )
    expect(getByText("Empty")).toBeTruthy()
    expect(getByText("No data")).toBeTruthy()
    expect(getByText("Reload")).toBeTruthy()
  })

  it("renders with custom image source", () => {
    const customImage = require("../../assets/images/sad-face.png")
    const { toJSON } = render(<EmptyState imageSource={customImage} />)
    expect(toJSON()).toBeTruthy()
  })

  it("applies custom container style", () => {
    const { toJSON } = render(
      <EmptyState style={{ backgroundColor: "red" }} />
    )
    expect(toJSON()).toBeTruthy()
  })
})


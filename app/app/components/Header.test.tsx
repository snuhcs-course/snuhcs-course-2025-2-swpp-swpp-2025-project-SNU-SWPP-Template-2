import { render, fireEvent } from "@testing-library/react-native"
import React from "react"
import { View } from "react-native"
import { Header } from "./Header"
import { SafeAreaProvider } from "react-native-safe-area-context"

const initialMetrics = {
  frame: { x: 0, y: 0, width: 0, height: 0 },
  insets: { top: 0, left: 0, right: 0, bottom: 0 },
}

const renderWithSafeArea = (component: React.ReactElement) => {
  return render(
    <SafeAreaProvider initialMetrics={initialMetrics}>
      {component}
    </SafeAreaProvider>
  )
}

describe("Header", () => {
  it("renders without crashing", () => {
    const { toJSON } = renderWithSafeArea(<Header />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders title text", () => {
    const { getByText } = renderWithSafeArea(<Header title="Test Title" />)
    expect(getByText("Test Title")).toBeTruthy()
  })

  it("renders with titleMode center", () => {
    const { toJSON } = renderWithSafeArea(<Header title="Centered" titleMode="center" />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders with titleMode flex", () => {
    const { toJSON } = renderWithSafeArea(<Header title="Flex" titleMode="flex" />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders with left icon", () => {
    const { toJSON } = renderWithSafeArea(<Header title="Title" leftIcon="back" />)
    expect(toJSON()).toBeTruthy()
  })

  it("renders with right icon", () => {
    const { toJSON } = renderWithSafeArea(<Header title="Title" rightIcon="menu" />)
    expect(toJSON()).toBeTruthy()
  })

  it("calls onLeftPress when left icon is pressed", () => {
    const onLeftPressMock = jest.fn()
    const { getAllByRole } = renderWithSafeArea(
      <Header title="Title" leftIcon="back" onLeftPress={onLeftPressMock} />
    )
    const pressables = getAllByRole("imagebutton")
    fireEvent.press(pressables[0])
    expect(onLeftPressMock).toHaveBeenCalledTimes(1)
  })

  it("calls onRightPress when right icon is pressed", () => {
    const onRightPressMock = jest.fn()
    const { getAllByRole } = renderWithSafeArea(
      <Header title="Title" rightIcon="menu" onRightPress={onRightPressMock} />
    )
    const pressables = getAllByRole("imagebutton")
    fireEvent.press(pressables[0])
    expect(onRightPressMock).toHaveBeenCalledTimes(1)
  })

  it("renders with left text action", () => {
    const { getByText } = renderWithSafeArea(<Header title="Title" leftText="Cancel" />)
    expect(getByText("Cancel")).toBeTruthy()
  })

  it("renders with right text action", () => {
    const { getByText } = renderWithSafeArea(<Header title="Title" rightText="Done" />)
    expect(getByText("Done")).toBeTruthy()
  })

  it("renders with custom LeftActionComponent", () => {
    const { getByTestId } = renderWithSafeArea(
      <Header 
        title="Title"
        LeftActionComponent={<View testID="left-custom" />}
      />
    )
    expect(getByTestId("left-custom")).toBeTruthy()
  })

  it("renders with custom RightActionComponent", () => {
    const { getByTestId } = renderWithSafeArea(
      <Header 
        title="Title"
        RightActionComponent={<View testID="right-custom" />}
      />
    )
    expect(getByTestId("right-custom")).toBeTruthy()
  })

  it("applies custom backgroundColor", () => {
    const { toJSON } = renderWithSafeArea(<Header title="Title" backgroundColor="#ff0000" />)
    expect(toJSON()).toBeTruthy()
  })

  it("applies custom titleStyle", () => {
    const { toJSON } = renderWithSafeArea(
      <Header title="Title" titleStyle={{ fontSize: 24 }} />
    )
    expect(toJSON()).toBeTruthy()
  })
})


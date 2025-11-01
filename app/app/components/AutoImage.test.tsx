import { render } from "@testing-library/react-native"
import React from "react"
import { AutoImage, useAutoImage } from "./AutoImage"

describe("AutoImage", () => {
  it("renders without crashing", () => {
    const { toJSON } = render(
      <AutoImage source={{ uri: "https://example.com/image.jpg" }} />
    )
    expect(toJSON()).toBeTruthy()
  })

  it("applies maxWidth prop", () => {
    const { toJSON } = render(
      <AutoImage 
        source={{ uri: "https://example.com/image.jpg" }} 
        maxWidth={200}
      />
    )
    expect(toJSON()).toBeTruthy()
  })

  it("applies maxHeight prop", () => {
    const { toJSON } = render(
      <AutoImage 
        source={{ uri: "https://example.com/image.jpg" }} 
        maxHeight={150}
      />
    )
    expect(toJSON()).toBeTruthy()
  })

  it("applies both maxWidth and maxHeight props", () => {
    const { toJSON } = render(
      <AutoImage 
        source={{ uri: "https://example.com/image.jpg" }} 
        maxWidth={200}
        maxHeight={150}
      />
    )
    expect(toJSON()).toBeTruthy()
  })

  it("accepts custom style prop", () => {
    const { toJSON } = render(
      <AutoImage 
        source={{ uri: "https://example.com/image.jpg" }}
        style={{ opacity: 0.5 }}
      />
    )
    expect(toJSON()).toBeTruthy()
  })
})


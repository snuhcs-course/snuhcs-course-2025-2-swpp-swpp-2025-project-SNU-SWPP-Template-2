import { render, fireEvent } from "@testing-library/react-native"
import React from "react"
import { FoodCard } from "./FoodCard"
import { FoodItem } from "../types/FoodTypes"

// Mock react-native-reanimated
jest.mock("react-native-reanimated", () => {
  const Reanimated = require("react-native-reanimated/mock")
  Reanimated.default.call = () => {}
  return Reanimated
})

// Mock lucide-react-native
jest.mock("lucide-react-native", () => ({
  Heart: "Heart",
  Bookmark: "Bookmark",
}))

const mockFoodItem: FoodItem = {
  id: 1,
  name: "Test Food",
  distance: "0.5km",
  image: "https://example.com/food.jpg",
  category: "Korean",
  allergens: ["peanuts", "dairy"],
  keywords: [],
}

describe("FoodCard", () => {
  it("renders without crashing", () => {
    const { toJSON } = render(
      <FoodCard 
        food={mockFoodItem}
        isLiked={false}
        isScrapped={false}
        onLike={jest.fn()}
        onScrap={jest.fn()}
      />
    )
    expect(toJSON()).toBeTruthy()
  })

  it("displays food name", () => {
    const { getByText } = render(
      <FoodCard 
        food={mockFoodItem}
        isLiked={false}
        isScrapped={false}
        onLike={jest.fn()}
        onScrap={jest.fn()}
      />
    )
    expect(getByText("Test Food")).toBeTruthy()
  })

  it("displays distance", () => {
    const { getByText } = render(
      <FoodCard 
        food={mockFoodItem}
        isLiked={false}
        isScrapped={false}
        onLike={jest.fn()}
        onScrap={jest.fn()}
      />
    )
    expect(getByText("0.5km")).toBeTruthy()
  })

  it("displays category", () => {
    const { getByText } = render(
      <FoodCard 
        food={mockFoodItem}
        isLiked={false}
        isScrapped={false}
        onLike={jest.fn()}
        onScrap={jest.fn()}
      />
    )
    expect(getByText("Korean")).toBeTruthy()
  })

  it("displays allergens", () => {
    const { getByText } = render(
      <FoodCard 
        food={mockFoodItem}
        isLiked={false}
        isScrapped={false}
        onLike={jest.fn()}
        onScrap={jest.fn()}
      />
    )
    expect(getByText("peanuts")).toBeTruthy()
    expect(getByText("dairy")).toBeTruthy()
  })

  it("calls onLike when like button is pressed", () => {
    const onLikeMock = jest.fn()
    const { UNSAFE_getAllByType } = render(
      <FoodCard 
        food={mockFoodItem}
        isLiked={false}
        isScrapped={false}
        onLike={onLikeMock}
        onScrap={jest.fn()}
      />
    )
    const { TouchableOpacity } = require("react-native")
    const buttons = UNSAFE_getAllByType(TouchableOpacity)
    fireEvent.press(buttons[0]) // First button is like
    expect(onLikeMock).toHaveBeenCalledTimes(1)
  })

  it("calls onScrap when scrap button is pressed", () => {
    const onScrapMock = jest.fn()
    const { UNSAFE_getAllByType } = render(
      <FoodCard 
        food={mockFoodItem}
        isLiked={false}
        isScrapped={false}
        onLike={jest.fn()}
        onScrap={onScrapMock}
      />
    )
    const { TouchableOpacity } = require("react-native")
    const buttons = UNSAFE_getAllByType(TouchableOpacity)
    fireEvent.press(buttons[1]) // Second button is scrap
    expect(onScrapMock).toHaveBeenCalledTimes(1)
  })

  it("applies scale prop", () => {
    const { toJSON } = render(
      <FoodCard 
        food={mockFoodItem}
        isLiked={false}
        isScrapped={false}
        onLike={jest.fn()}
        onScrap={jest.fn()}
        scale={0.8}
      />
    )
    expect(toJSON()).toBeTruthy()
  })

  it("applies maxWidth prop", () => {
    const { toJSON } = render(
      <FoodCard 
        food={mockFoodItem}
        isLiked={false}
        isScrapped={false}
        onLike={jest.fn()}
        onScrap={jest.fn()}
        maxWidth={300}
      />
    )
    expect(toJSON()).toBeTruthy()
  })

  it("applies maxHeight prop", () => {
    const { toJSON } = render(
      <FoodCard 
        food={mockFoodItem}
        isLiked={false}
        isScrapped={false}
        onLike={jest.fn()}
        onScrap={jest.fn()}
        maxHeight={400}
      />
    )
    expect(toJSON()).toBeTruthy()
  })
})


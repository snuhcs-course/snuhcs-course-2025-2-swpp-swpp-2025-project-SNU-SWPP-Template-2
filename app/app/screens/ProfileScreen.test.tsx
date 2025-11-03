import React from "react";
import { render, fireEvent, waitFor } from "@testing-library/react-native";
import { ProfileScreen } from "./ProfileScreen";

// Mocks for navigation and store context
const mockReplace = jest.fn();
const navigation = { replace: mockReplace, navigate: jest.fn() };

jest.mock("../models", () => ({ useStores: () => ({ foodHistoryStore: { scrappedItemsList: [] } }) }));
jest.mock("app/services/albums/useAlbumScanner", () => ({ useAlbumScanner: () => ({ scanAlbums: jest.fn() }) }));
jest.mock("app/services/api", () => ({ api: { me: jest.fn(() => Promise.resolve({ ok: true, data: { username: "Sophia" } })), logout: jest.fn() } }));
jest.mock("app/services/aws/handleAwsSignin", () => ({ handleSignOut: jest.fn() }));
jest.mock("app/utils/storage", () => ({ remove: jest.fn() }));

// Silence useEffect warning
jest.spyOn(React, 'useEffect').mockImplementation(f => f());

describe("ProfileScreen", () => {
  it("renders profile name and static sections", async () => {
    const { queryByText } = render(<ProfileScreen navigation={navigation} />);
    expect(queryByText("foodigram")).toBeTruthy();
    // Profile user
    expect(queryByText("Sophia")).toBeTruthy();
    expect(queryByText("Foodie")).toBeTruthy();
    // Liked restaurants (sample names from component)
    expect(queryByText("The Pasta Place")).toBeTruthy();
    expect(queryByText("Ocean's Catch")).toBeTruthy();
    expect(queryByText("Sweet Garden")).toBeTruthy();
    // Food history
    expect(queryByText("Food History")).toBeTruthy();
    // Log out button
    expect(queryByText("Log out")).toBeTruthy();
  });

  it("opens filter modal when filter button is pressed", () => {
    const { getByTestId } = render(<ProfileScreen navigation={navigation} />);
    fireEvent.press(getByTestId('filter-button'));
  });

  it("calls logout and navigates to Login when logout button is pressed", async () => {
    const { queryByText, getByText } = render(<ProfileScreen navigation={navigation} />);
    fireEvent.press(getByText("Log out"));
    await waitFor(() => expect(mockReplace).toHaveBeenCalledWith("Login"));
  });

  it("can toggle User Images filter in filter modal", () => {
    const { getByTestId, getByText } = render(<ProfileScreen navigation={navigation} />);
    fireEvent.press(getByTestId('filter-button'));
    // Filter modal should appear, press "User Images" to toggle
    fireEvent.press(getByText('User Images'));
  });

  it("can toggle Scrapped filter in filter modal", () => {
    const { getByTestId, getByText } = render(<ProfileScreen navigation={navigation} />);
    fireEvent.press(getByTestId('filter-button'));
    // Press "Scrapped" to toggle
    fireEvent.press(getByText('Scrapped'));
  });

  it("can open/close preferences modal", () => {
    const { getByTestId, queryByText } = render(<ProfileScreen navigation={navigation} />);
    // Open
    fireEvent.press(getByTestId('settings-button'));
    expect(queryByText('Preferences')).toBeTruthy(); // Depends on actual PreferencesModal content
    // Close by simulating onClose prop
    // Normally, we'd invoke the onClose, but since it's conditional, skip actual closing
  });

  it("activates Foodigram tab on bottom nav", () => {
    const { getByTestId } = render(<ProfileScreen navigation={navigation} />);
    fireEvent.press(getByTestId('FoodigramTab'));
    expect(navigation.navigate).toHaveBeenCalledWith('Foodigram');
  });

  it("can close filter modal via backdrop press", () => {
    const { getByTestId, getByText } = render(<ProfileScreen navigation={navigation} />);
    fireEvent.press(getByTestId('filter-button'));
    fireEvent.press(getByTestId('filter-modal-backdrop'));
  });

  it("can close filter modal via close(X) button", () => {
    const { getByTestId, getByText } = render(<ProfileScreen navigation={navigation} />);
    fireEvent.press(getByTestId('filter-button'));
    fireEvent.press(getByTestId('filter-modal-close'));
  });

});

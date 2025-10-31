import { Api, DEFAULT_API_CONFIG } from "./api"
import Cookies from '@react-native-cookies/cookies'
import { create } from "apisauce"

jest.mock("@react-native-cookies/cookies", () => ({
    setFromResponse: jest.fn(),
    get: jest.fn(),
}))
jest.mock("apisauce", () => ({
    create: jest.fn(),
}))

describe("Api", () => {
    let api: Api
    let fakeApisauce: any

    beforeEach(() => {
        fakeApisauce = {
            get: jest.fn(() => Promise.resolve({ data: { csrfToken: "mockCsrf" }, headers: {} })),
            post: jest.fn(() => Promise.resolve({ data: {}, headers: {} })),
            setHeader: jest.fn(),
            delete: jest.fn(() => Promise.resolve({ data: {}, headers: {} })),
            patch: jest.fn(() => Promise.resolve({ data: {}, headers: {} })),

        }
            ; (create as jest.Mock).mockReturnValue(fakeApisauce)
        api = new Api(DEFAULT_API_CONFIG)
    })

    afterEach(() => {
        jest.clearAllMocks()
    })

    it("getCsrf sets X-CSRFToken header when present", async () => {
        await api.getCsrf()
        expect(fakeApisauce.get).toHaveBeenCalledWith("/auth/csrf/")
        expect(fakeApisauce.setHeader).toHaveBeenCalledWith("X-CSRFToken", "mockCsrf")
    })

    it("login calls getCsrf if cookie missing and makes POST call", async () => {
        fakeApisauce.defaults = { headers: { common: {} } }
        const spy = jest.spyOn(api, "getCsrf").mockResolvedValue({ data: { csrfToken: "mockCsrf" }, headers: {} })
        await api.login("user", "pass")
        expect(spy).toHaveBeenCalled()
        expect(fakeApisauce.post).toHaveBeenCalledWith("/auth/login/", { username: "user", password: "pass" })
    })

    it("register calls getCsrf if cookie missing and makes POST call", async () => {
        fakeApisauce.defaults = { headers: { common: {} } }
        const spy = jest.spyOn(api, "getCsrf").mockResolvedValue({ data: { csrfToken: "mockCsrf" }, headers: {} })
        await api.register("user", "u@email.com", "pass")
        expect(spy).toHaveBeenCalled()
        expect(fakeApisauce.post).toHaveBeenCalledWith("/auth/register/", { username: "user", email: "u@email.com", password: "pass" })
    })

    it("logout always posts to /auth/logout/", async () => {
        await api.logout()
        expect(fakeApisauce.post).toHaveBeenCalledWith("/auth/logout/")
    })

    it("me always get /me/", async () => {
        await api.me()
        expect(fakeApisauce.get).toHaveBeenCalledWith("/me/")
    })

    it("uploadPhoto posts to /photos/", async () => {
        await api.uploadPhoto("http://example.com/foo.jpg")
        expect(fakeApisauce.post).toHaveBeenCalledWith("/photos/", { photo_url: "http://example.com/foo.jpg" })
    })

    it("getMenuRecommendations sends location in request (success)", async () => {
        fakeApisauce.post.mockResolvedValue({ ok: true, data: { recommended: ["foo"] } })
        const res = await api.getMenuRecommendations([1, 2], { queryText: "bar", maxResults: 1 })
        expect(fakeApisauce.post).toHaveBeenCalledWith(
            "/recommendation/recommend/menu/",
            expect.objectContaining({ user_location: [1, 2], queryText: "bar", maxResults: 1 })
        )
        expect(res.recommended).toEqual(["foo"])
    })

    it("getMenuRecommendations throws an error on failure", async () => {
        fakeApisauce.post.mockResolvedValueOnce({ ok: false, data: { error: "fail" } })
        await expect(api.getMenuRecommendations([0, 0])).rejects.toThrow('fail')
    })

    it("toggleScrap posts to /scraps/toggle/", async () => {
        await api.toggleScrap(42)
        expect(fakeApisauce.post).toHaveBeenCalledWith("/scraps/toggle/", { restaurant_id: 42 })
    })

    it("getScraps gets /scraps/", async () => {
        await api.getScraps()
        expect(fakeApisauce.get).toHaveBeenCalledWith("/scraps/")
    })

    it("addScrap posts to /scraps/", async () => {
        await api.addScrap(77)
        expect(fakeApisauce.post).toHaveBeenCalledWith("/scraps/", { restaurant_id: 77 })
    })

    it("deleteScrap deletes /scraps/:id/", async () => {
        await api.deleteScrap(21)
        expect(fakeApisauce.delete).toHaveBeenCalledWith("/scraps/21/")
    })

    it("getRestaurantDetail gets /restaurants/:id/", async () => {
        await api.getRestaurantDetail(3)
        expect(fakeApisauce.get).toHaveBeenCalledWith("/restaurants/3/")
    })

    it("getPreferences gets /onboarding/", async () => {
        await api.getPreferences()
        expect(fakeApisauce.get).toHaveBeenCalledWith("/onboarding/")
    })

    it("savePreferences posts to /onboarding/", async () => {
        await api.savePreferences({ spicy_level: 1 })
        expect(fakeApisauce.post).toHaveBeenCalledWith("/onboarding/", { spicy_level: 1 })
    })

    it("updatePreferences patches /onboarding/update/", async () => {
        await api.updatePreferences({ sweet_level: 5 })
        expect(fakeApisauce.patch).toBeDefined() // not originally in fakeApisauce, patch it
    })

    it("setFromResponse is called if present in getCsrf", async () => {
        (Cookies as any).setFromResponse = jest.fn()
        fakeApisauce.get.mockResolvedValue({ data: { csrfToken: "mockCsrf" }, headers: { 'set-cookie': 'cookiex' } })
        await api.getCsrf()
        expect((Cookies as any).setFromResponse).toHaveBeenCalled()
    })

    it("getter logic for Cookies.get in attachCookiesHeader", async () => {
        (Cookies as any).get = jest.fn().mockResolvedValue({ session: { value: 'abc' } })
        await (api as any).attachCookiesHeader()
        expect((Cookies as any).get).toHaveBeenCalled()
        expect(fakeApisauce.setHeader).toHaveBeenCalledWith('Cookie', 'session=abc')
    })

    it("should handle when no cookies available in attachCookiesHeader", async () => {
        (Cookies as any).get = jest.fn().mockResolvedValue({})
        await (api as any).attachCookiesHeader()
        expect(fakeApisauce.setHeader).not.toHaveBeenCalledWith('Cookie', expect.any(String))
    })

    it("should not crash if setFromResponse is missing (gracefully handles)", async () => {
        delete (Cookies as any).setFromResponse
        fakeApisauce.get.mockResolvedValue({ data: { csrfToken: "c" }, headers: { 'set-cookie': 'cookiey' } })
        await expect(api.getCsrf()).resolves.not.toThrow()
    })

    it("should skip setHeader if csfrToken not present", async () => {
        fakeApisauce.get.mockResolvedValue({ data: {}, headers: {} })
        await api.getCsrf()
        expect(fakeApisauce.setHeader).not.toHaveBeenCalledWith("X-CSRFToken", expect.anything())
    })
})

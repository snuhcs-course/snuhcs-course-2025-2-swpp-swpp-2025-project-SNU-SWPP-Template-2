/**
 * authController.test.js
 * Sumdays - Authentication Controllers Coverage Test
 */

const request = require("supertest");

// 모든 외부 모듈 mocking
jest.mock("bcrypt", () => ({
    compare: jest.fn(),
    hash: jest.fn(),
}));

jest.mock("jsonwebtoken", () => ({
    sign: jest.fn().mockReturnValue("mocked.jwt.token"),
}));

jest.mock("../db/db", () => ({
    pool: {
        query: jest.fn(),
    },
}));

const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const { pool } = require("../db/db");

const authController = require("../controllers/authController");

// mock response 객체 생성
const mockResponse = () => {
    const res = {};
    res.status = jest.fn().mockReturnValue(res);
    res.json = jest.fn().mockReturnValue(res);
    return res;
};

describe("Auth Controller Tests (Coverage 90%↑)", () => {

    // -----------------------------------------------------------
    // LOGIN TESTS
    // -----------------------------------------------------------

    test("login - missing fields", async () => {
        const req = { body: { email: "", password: "" } };
        const res = mockResponse();

        await authController.login(req, res);

        expect(res.status).toHaveBeenCalledWith(400);
    });

    test("login - user not found", async () => {
        pool.query.mockResolvedValue([[]]); // no user
        const req = { body: { email: "a@a.com", password: "1234" } };
        const res = mockResponse();

        await authController.login(req, res);

        expect(res.status).toHaveBeenCalledWith(401);
    });

    test("login - wrong password", async () => {
        pool.query.mockResolvedValue([[{
            id: 1,
            email: "a@a.com",
            password_hash: "hashed",
            nickname: "민규"
        }]]);

        bcrypt.compare.mockResolvedValue(false);

        const req = { body: { email: "a@a.com", password: "wrong" } };
        const res = mockResponse();

        await authController.login(req, res);

        expect(res.status).toHaveBeenCalledWith(401);
    });

    test("login - success", async () => {
        pool.query.mockResolvedValue([[{
            id: 1,
            email: "a@a.com",
            password_hash: "hashed_pw",
            nickname: "민규"
        }]]);

        bcrypt.compare.mockResolvedValue(true);

        const req = { body: { email: "a@a.com", password: "1234" } };
        const res = mockResponse();

        await authController.login(req, res);

        expect(res.status).toHaveBeenCalledWith(200);
        expect(jwt.sign).toHaveBeenCalled();
    });

    test("login - internal server error", async () => {
        pool.query.mockRejectedValue(new Error("DB ERR"));

        const req = { body: { email: "a@a.com", password: "1234" } };
        const res = mockResponse();

        await authController.login(req, res);

        expect(res.status).toHaveBeenCalledWith(500);
    });

    // -----------------------------------------------------------
    // SIGNUP TESTS
    // -----------------------------------------------------------

    test("signup - missing fields", async () => {
        const req = { body: { nickname: "", email: "", password: "" } };
        const res = mockResponse();

        await authController.signup(req, res);

        expect(res.status).toHaveBeenCalledWith(400);
    });

    test("signup - email duplicate", async () => {
        pool.query.mockResolvedValue([[{ email: "a@a.com" }]]);

        const req = { body: { nickname: "민규", email: "a@a.com", password: "1234" } };
        const res = mockResponse();

        await authController.signup(req, res);

        expect(res.status).toHaveBeenCalledWith(409);
    });

    test("signup - success", async () => {
        bcrypt.hash.mockResolvedValue("hashed_pw");
        pool.query
            .mockResolvedValueOnce([[]])     // email check
            .mockResolvedValueOnce([{ insertId: 1 }]); // insert

        const req = { body: { nickname: "민규", email: "a@a.com", password: "1234" } };
        const res = mockResponse();

        await authController.signup(req, res);

        expect(res.status).toHaveBeenCalledWith(201);
    });

    test("signup - error", async () => {
        pool.query.mockRejectedValue(new Error("ERR"));

        const req = { body: { nickname: "민규", email: "a@a.com", password: "1234" } };
        const res = mockResponse();

        await authController.signup(req, res);

        expect(res.status).toHaveBeenCalledWith(500);
    });

    // -----------------------------------------------------------
    // CHANGE PASSWORD TESTS
    // -----------------------------------------------------------

    test("changePassword - missing fields", async () => {
        const req = { user: { userId: 1 }, body: { currentPassword: "", newPassword: "" } };
        const res = mockResponse();

        await authController.changePassword(req, res);

        expect(res.status).toHaveBeenCalledWith(400);
    });

    test("changePassword - user not found", async () => {
        pool.query.mockResolvedValue([[]]);

        const req = { user: { userId: 1 }, body: { currentPassword: "1234", newPassword: "5678" } };
        const res = mockResponse();

        await authController.changePassword(req, res);

        expect(res.status).toHaveBeenCalledWith(404);
    });

    test("changePassword - wrong current password", async () => {
        pool.query.mockResolvedValue([[{ password_hash: "hashed" }]]);
        bcrypt.compare.mockResolvedValue(false);

        const req = { user: { userId: 1 }, body: { currentPassword: "x", newPassword: "y" } };
        const res = mockResponse();

        await authController.changePassword(req, res);

        expect(res.status).toHaveBeenCalledWith(401);
    });

    test("changePassword - success", async () => {
        pool.query
            .mockResolvedValueOnce([[{ password_hash: "hashed" }]]) // select
            .mockResolvedValueOnce([{}]);                          // update
        bcrypt.compare.mockResolvedValue(true);
        bcrypt.hash.mockResolvedValue("new_hash");

        const req = { user: { userId: 1 }, body: { currentPassword: "1", newPassword: "2" } };
        const res = mockResponse();

        await authController.changePassword(req, res);

        expect(res.status).toHaveBeenCalledWith(200);
    });

    test("changePassword - error", async () => {
        pool.query.mockRejectedValue(new Error("ERR"));

        const req = { user: { userId: 1 }, body: { currentPassword: "a", newPassword: "b" } };
        const res = mockResponse();

        await authController.changePassword(req, res);

        expect(res.status).toHaveBeenCalledWith(500);
    });

    // -----------------------------------------------------------
    // CHANGE NICKNAME TESTS
    // -----------------------------------------------------------

    test("changeNickname - missing field", async () => {
        const req = { user: { userId: 1 }, body: { newNickname: "" } };
        const res = mockResponse();

        await authController.changeNickname(req, res);

        expect(res.status).toHaveBeenCalledWith(400);
    });

    test("changeNickname - duplicate nickname", async () => {
        pool.query.mockResolvedValue([[{ id: 2 }]]);

        const req = { user: { userId: 1 }, body: { newNickname: "민규" } };
        const res = mockResponse();

        await authController.changeNickname(req, res);

        expect(res.status).toHaveBeenCalledWith(409);
    });

    test("changeNickname - success", async () => {
        pool.query
            .mockResolvedValueOnce([[]]) // nickname check
            .mockResolvedValueOnce([{}]); // update

        const req = { user: { userId: 1 }, body: { newNickname: "새닉" } };
        const res = mockResponse();

        await authController.changeNickname(req, res);

        expect(res.status).toHaveBeenCalledWith(200);
    });

    test("changeNickname - error", async () => {
        pool.query.mockRejectedValue(new Error("ERR"));

        const req = { user: { userId: 1 }, body: { newNickname: "새닉" } };
        const res = mockResponse();

        await authController.changeNickname(req, res);

        expect(res.status).toHaveBeenCalledWith(500);
    });

});

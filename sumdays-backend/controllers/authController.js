const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');

// =================================================================
// ★★★ 중요: 이 비밀 키와 DB 정보는 절대로 코드에 하드코딩하면 안 됩니다. ★★★
// 실제 프로덕션 환경에서는 .env 파일과 같은 환경 변수로 안전하게 관리해야 합니다.
// =================================================================
const JWT_SECRET = 'Sumdays_Project_Super_Secret_Key_!@#$%^&*()';

const db = require('../db/db');

exports.login = async (req, res) => {
    const { email, password } = req.body;

    // console.log(`[로그인 시도] 이메일: ${email}`);

    // 입력 값 유효성 검사
    if (!email || !password) {
        return res.status(400).json({
            success: false,
            message: '이메일과 비밀번호를 모두 입력해주세요.'
        });
    }
    
    try {
        const sql = 'SELECT id, email, password_hash FROM users WHERE email = ?';
        const [rows] = await db.query(sql, [email]);
        const user = rows[0]; 

        // no exist 
        if (!user) {
            // 사용자가 존재하지 않는 경우
            // console.log(`[로그인 실패] 존재하지 않는 이메일: ${email}`);
            // 보안을 위해 "이메일이 없음" 대신 "정보가 일치하지 않음"으로 응답합니다.
            return res.status(401).json({
                success: false,
                message: '이메일 또는 비밀번호가 올바르지 않습니다.'
            });
        }

        // 2. 사용자가 입력한 비밀번호와 DB에 저장된 해시된 비밀번호 비교
        const isPasswordMatch = await bcrypt.compare(password, user.password_hash);

        if (isPasswordMatch) {
            // 비밀번호 일치 (로그인 성공)
            // console.log(`[로그인 성공] 사용자 ID: ${user.id}`);

            // JWT 페이로드(Payload) 생성
            const payload = {
                userId: user.id,
                email: user.email
            };

            // JWT 생성 (유효 기간: 7일)
            const token = jwt.sign(payload, JWT_SECRET, { expiresIn: '7d' });

            // 클라이언트에게 성공 응답 전송
            res.status(200).json({
                success: true,
                message: '로그인 성공',
                userId: user.id,
                token: token
            });

        } else {
            // 비밀번호 불일치
            // console.log(`[로그인 실패] 비밀번호 불일치: ${email}, ${password}, ${user.password_hash}`);
            return res.status(401).json({
                success: false,
                message: '이메일 또는 비밀번호가 올바르지 않습니다.'
            });
        }

    } catch (error) {
        // console.error('[서버 오류] 로그인 처리 중 에러 발생:', error);
        res.status(500).json({
            success: false,
            message: '서버 내부 오류가 발생했습니다.'
        });
    }
};

// ★★★ POST /api/signup: 회원가입 요청을 처리하는 엔드포인트 추가 ★★★
exports.signup = async (req, res) => {
    const { nickname, email, password } = req.body;
    // console.log(`[회원가입 시도] 이메일: ${email}`);

    // 1. 입력 값 유효성 검사
    if (!nickname || !email || !password) {
        return res.status(400).json({ success: false, message: '모든 정보를 입력해주세요.' });
    }
    try {
        // 1. 이메일 중복 확인
        const [existingUsers] = await db.query('SELECT email FROM users WHERE email = ?', [email]);
        if (existingUsers.length > 0) {
            return res.status(409).json({ success: false, message: '이미 사용 중인 이메일입니다.' });
        }

        // 2. 비밀번호 해시 생성
        const saltRounds = 10; // 해시 복잡도
        const hashedPassword = await bcrypt.hash(password, saltRounds);

        // 3. 새로운 사용자 정보를 DB에 저장
        const sql = 'INSERT INTO users (nickname, email, password_hash) VALUES (?, ?, ?)';
        await db.query(sql, [nickname, email, hashedPassword]);

        // console.log(`[회원가입 성공] 이메일: ${email}`);
        res.status(201).json({ success: true, message: '회원가입이 성공적으로 완료되었습니다.' });

    } catch (error) {
        // console.error('[서버 오류] 회원가입 처리 중 에러 발생:', error);
        res.status(500).json({ success: false, message: '서버 내부 오류가 발생했습니다.' });
    } 
};

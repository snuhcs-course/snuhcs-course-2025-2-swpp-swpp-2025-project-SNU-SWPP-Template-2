// 1. 필요한 라이브러리들을 가져옵니다.
const express = require('express');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const mysql = require('mysql2/promise'); // MySQL 데이터베이스와 비동기 통신을 위한 라이브러리

// 2. Express 애플리케이션을 생성합니다.
const app = express();
const port = 3000; // 안드로이드 앱의 RetrofitClient와 동일한 포트 번호

// 3. Middlewares 설정
// express.json()은 들어오는 요청의 본문(body)을 JSON으로 파싱해줍니다.
app.use(express.json());

// =================================================================
// ★★★ 중요: 이 비밀 키와 DB 정보는 절대로 코드에 하드코딩하면 안 됩니다. ★★★
// 실제 프로덕션 환경에서는 .env 파일과 같은 환경 변수로 안전하게 관리해야 합니다.
// =================================================================
const JWT_SECRET = 'Sumdays_Project_Super_Secret_Key_!@#$%^&*()';

const dbConfig = {
    host: 'sumdays-database.c502cecwedjd.ap-northeast-2.rds.amazonaws.com',      // 예: sumdays-db.abcdefg12345.ap-northeast-2.rds.amazonaws.com
    user: 'swpp_team15',       // 예: admin
    password: 'aoij*i9!jUjkm',   // RDS 생성 시 설정한 마스터 암호
    database: 'login',         // 연결할 데이터베이스 이름
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
};

// 데이터베이스 커넥션 풀(Pool) 생성. 여러 요청을 효율적으로 처리합니다.
const pool = mysql.createPool(dbConfig);
// =================================================================


// 4. API 라우트(Route) 정의
// POST /api/login: 로그인 요청을 처리하는 엔드포인트
app.post('/api/login', async (req, res) => {
    // 클라이언트가 보낸 email과 password를 요청 본문(body)에서 추출합니다.
    const { email, password } = req.body;

    console.log(`[로그인 시도] 이메일: ${email}`);

    // 입력 값 유효성 검사
    if (!email || !password) {
        return res.status(400).json({
            success: false,
            message: '이메일과 비밀번호를 모두 입력해주세요.'
        });
    }

    let connection;
    try {
        // 커넥션 풀에서 연결을 하나 가져옵니다.
        connection = await pool.getConnection();

        // ★★★★★★★★★★★ 실제 로그인 쿼리를 실행하는 부분 ★★★★★★★★★★★
        // 1. DB에서 이메일로 사용자 정보 조회
        // SQL Injection 공격을 방지하기 위해 `?`를 사용한 파라미터 바인딩은 필수입니다.
        const sql = 'SELECT user_id, email, password_hash FROM users WHERE email = ?';
        const [rows] = await connection.execute(sql, [email]);
        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

        const user = rows[0]; // 쿼리 결과는 배열이므로 첫 번째 요소를 가져옵니다.

        if (!user) {
            // 사용자가 존재하지 않는 경우
            console.log(`[로그인 실패] 존재하지 않는 이메일: ${email}`);
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
            console.log(`[로그인 성공] 사용자 ID: ${user.user_id}`);

            // JWT 페이로드(Payload) 생성
            const payload = {
                userId: user.user_id,
                email: user.email
            };

            // JWT 생성 (유효 기간: 7일)
            const token = jwt.sign(payload, JWT_SECRET, { expiresIn: '7d' });

            // 클라이언트에게 성공 응답 전송
            res.status(200).json({
                success: true,
                message: '로그인 성공',
                userId: user.user_id,
                token: token
            });

        } else {
            // 비밀번호 불일치
            console.log(`[로그인 실패] 비밀번호 불일치: ${email}, ${password}, ${user.password_hash}`);
            return res.status(401).json({
                success: false,
                message: '이메일 또는 비밀번호가 올바르지 않습니다.'
            });
        }

    } catch (error) {
        console.error('[서버 오류] 로그인 처리 중 에러 발생:', error);
        res.status(500).json({
            success: false,
            message: '서버 내부 오류가 발생했습니다.'
        });
    } finally {
        // 사용한 데이터베이스 연결을 반드시 풀에 반환해야 합니다.
        if (connection) {
            connection.release();
        }
    }
});

// ★★★ POST /api/signup: 회원가입 요청을 처리하는 엔드포인트 추가 ★★★
app.post('/api/signup', async (req, res) => {
    const { nickname, email, password } = req.body;
    console.log(`[회원가입 시도] 이메일: ${email}`);

    // 1. 입력 값 유효성 검사
    if (!nickname || !email || !password) {
        return res.status(400).json({ success: false, message: '모든 정보를 입력해주세요.' });
    }

    let connection;
    try {
        connection = await pool.getConnection();

        // 2. 이메일 중복 확인
        const [existingUsers] = await connection.execute('SELECT email FROM users WHERE email = ?', [email]);
        if (existingUsers.length > 0) {
            return res.status(409).json({ success: false, message: '이미 사용 중인 이메일입니다.' });
        }

        // 3. 비밀번호 해시 생성
        const saltRounds = 10; // 해시 복잡도
        const hashedPassword = await bcrypt.hash(password, saltRounds);

        // 4. 새로운 사용자 정보를 DB에 저장
        const sql = 'INSERT INTO users (nickname, email, password_hash) VALUES (?, ?, ?)';
        await connection.execute(sql, [nickname, email, hashedPassword]);

        console.log(`[회원가입 성공] 이메일: ${email}`);
        res.status(201).json({ success: true, message: '회원가입이 성공적으로 완료되었습니다.' });

    } catch (error) {
        console.error('[서버 오류] 회원가입 처리 중 에러 발생:', error);
        res.status(500).json({ success: false, message: '서버 내부 오류가 발생했습니다.' });
    } finally {
        if (connection) connection.release(); // 사용한 커넥션 반환
    }
});


// 5. 서버 실행
app.listen(port, () => {
    console.log(`Sumdays 백엔드 서버가 포트 ${port}번에서 실행 중입니다.`);
    console.log('안드로이드 앱의 요청을 기다리고 있습니다...');
});

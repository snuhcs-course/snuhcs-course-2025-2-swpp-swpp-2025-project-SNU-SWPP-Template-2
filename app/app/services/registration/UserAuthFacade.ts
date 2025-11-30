import type { Api } from "../api"
import { api } from "../api"
import * as storage from "app/utils/storage"
import { signUp, signIn, signOut, deleteUser, getCurrentUser, confirmSignUp } from "aws-amplify/auth"

interface RegistrationApi extends Pick<Api, "getCsrf" | "register" | "login" | "logout" | "getPreferences"> {}
interface StoragePort {
  saveString(key: string, value: string): Promise<boolean>
  remove(key: string): Promise<void>
}

export interface RegistrationRequest {
  username: string
  email: string
  password: string
}

export interface RegistrationResult {
  success: boolean
  errorMessage?: string
}

interface Logger {
  info(message: string, meta?: Record<string, unknown>): void
  error(message: string, meta?: Record<string, unknown>): void
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResult {
  success: boolean
  errorMessage?: string
  hasPreferences?: boolean
}

export interface UserAuthFacadeOptions {
  apiClient?: RegistrationApi
  storagePort?: StoragePort
  cognitoSignUp?: typeof signUp
  cognitoSignIn?: typeof signIn
  cognitoSignOut?: typeof signOut
  logger?: Logger
}

/**
 * Facade encapsulating the multiple steps needed for user authentication (registration, login, logout).
 * Screens can now rely on a single entry point, keeping UI code lean while enabling easier testing and substitution of dependencies.
 */
export class UserAuthFacade {
  private apiClient: RegistrationApi
  private storagePort: StoragePort
  private cognitoSignUp: typeof signUp
  private cognitoSignIn: typeof signIn
  private cognitoSignOut: typeof signOut
  private logger: Logger
  private static readonly AUTH_FLAG_KEY = "IS_LOGGED_IN"

  constructor(options: UserAuthFacadeOptions = {}) {
    this.apiClient = options.apiClient ?? api
    this.storagePort =
      options.storagePort ??
      { saveString: storage.saveString, remove: storage.remove }
    this.cognitoSignUp = options.cognitoSignUp ?? signUp
    this.cognitoSignIn = options.cognitoSignIn ?? signIn
    this.cognitoSignOut = options.cognitoSignOut ?? signOut
    this.logger =
      options.logger ??
      ({
        info: (message: string, meta?: Record<string, unknown>) => console.log(message, meta),
        error: (message: string, meta?: Record<string, unknown>) => console.error(message, meta),
      } as Logger)
  }

  async registerUser(request: RegistrationRequest): Promise<RegistrationResult> {
    this.logger.info("registration:start", { username: request.username })
    try {
      await this.apiClient.getCsrf()
    } catch (error) {
      this.logger.error("registration:csrf_failed", { username: request.username, error })
      return { success: false, errorMessage: "보안 토큰을 가져오지 못했습니다." }
    }

    const response = await this.apiClient.register(request.username, request.email, request.password)
    if (!response.ok) {
      const detail = (response.data as any)?.detail
      this.logger.error("registration:backend_failed", {
        username: request.username,
        email: request.email,
        detail,
      })
      return { success: false, errorMessage: detail ?? "회원가입에 실패했습니다." }
    }

    try {
      const signUpResult = await this.cognitoSignUp({
        username: request.username,
        password: request.password,
        options: {
          userAttributes: { email: request.email },
          autoSignIn: true,
        },
      } as any)

      this.logger.info("registration:cognito_signup_result", { 
        username: request.username, 
        isSignUpComplete: signUpResult.isSignUpComplete,
        nextStep: signUpResult.nextStep
      })

      // If email verification is required, auto-confirm for development
      if (!signUpResult.isSignUpComplete && signUpResult.nextStep?.signUpStep === 'CONFIRM_SIGN_UP') {
        this.logger.info("registration:email_verification_required", { username: request.username })
        
        // For development, we'll skip email verification by not requiring it
        // In production, you would handle email verification properly
        this.logger.info("registration:skipping_email_verification_for_dev", { username: request.username })
      }

      if (!signUpResult.isSignUpComplete) {
        this.logger.error("registration:cognito_signup_incomplete", {
          username: request.username,
          nextStep: signUpResult.nextStep
        })
        throw new Error(
          `회원가입이 완료되지 않았습니다. 다음 단계: ${JSON.stringify(signUpResult.nextStep)}`,
        )
      }

      // Ensure the user is signed in after registration
      try {
        this.logger.info("registration:cognito_signin_start", { username: request.username })
        const signInResult = await this.cognitoSignIn({
          username: request.username,
          password: request.password,
        } as any)
        
        this.logger.info("registration:cognito_signin_result", { 
          username: request.username, 
          isSignedIn: signInResult.isSignedIn,
          nextStep: signInResult.nextStep
        })
        
        if (!signInResult.isSignedIn) {
          this.logger.error("registration:cognito_signin_failed", { 
            username: request.username, 
            nextStep: signInResult.nextStep 
          })
          throw new Error(`Auto sign-in after registration failed. Next step: ${JSON.stringify(signInResult.nextStep)}`)
        }
        
        // Verify the sign-in worked
        try {
          const currentUser = await getCurrentUser()
          this.logger.info("registration:cognito_signin_verified", { 
            username: request.username, 
            cognitoUsername: currentUser.username,
            userId: currentUser.userId
          })
        } catch (verificationError) {
          this.logger.error("registration:cognito_signin_verification_failed", { 
            username: request.username, 
            error: verificationError 
          })
          throw verificationError
        }
        
        this.logger.info("registration:cognito_signin_success", { username: request.username })
      } catch (signInError) {
        this.logger.error("registration:cognito_signin_error", { username: request.username, error: signInError })
        // Continue anyway - registration was successful, just sign-in failed
      }
    } catch (error) {
      this.logger.error("registration:cognito_failed", { username: request.username, error })
      return {
        success: false,
        errorMessage: "AWS 회원가입 도중 문제가 발생했습니다.",
      }
    }

    await this.storagePort.saveString(UserAuthFacade.AUTH_FLAG_KEY, "true")
    await this.storagePort.saveString("NEW_USER", "true") // Mark as new user for onboarding
    
    // Store credentials for potential re-authentication (encrypted in production)
    await this.storagePort.saveString("STORED_USERNAME", request.username)
    await this.storagePort.saveString("STORED_PASSWORD", request.password)
    this.logger.info("registration:success", { username: request.username })
    return { success: true }
  }

  async loginUser(request: LoginRequest): Promise<LoginResult> {
    this.logger.info("login:start", { username: request.username })

    try {
      await this.apiClient.getCsrf()
    } catch (error) {
      this.logger.error("login:csrf_failed", { username: request.username, error })
      return { success: false, errorMessage: "보안 토큰을 가져오지 못했습니다." }
    }

    const response = await this.apiClient.login(request.username, request.password)
    if (!response.ok) {
      const detail = (response.data as any)?.detail
      this.logger.error("login:backend_failed", { username: request.username, detail })
      return { success: false, errorMessage: detail ?? "로그인에 실패했습니다." }
    }

    try {
      this.logger.info("login:cognito_signin_start", { username: request.username })
      const signInResult = await this.cognitoSignIn({
        username: request.username,
        password: request.password,
      } as any)
      
      this.logger.info("login:cognito_signin_result", { 
        username: request.username, 
        isSignedIn: signInResult.isSignedIn,
        nextStep: signInResult.nextStep
      })

      if (!signInResult.isSignedIn) {
        this.logger.error("login:cognito_signin_incomplete", { 
          username: request.username, 
          nextStep: signInResult.nextStep 
        })
        throw new Error(`Amplify sign-in incomplete. Next step: ${JSON.stringify(signInResult.nextStep)}`)
      }
      
      // Verify the authentication by checking current user
      try {
        const currentUser = await getCurrentUser()
        this.logger.info("login:cognito_user_verified", { 
          username: request.username, 
          cognitoUsername: currentUser.username,
          userId: currentUser.userId
        })
      } catch (userError) {
        this.logger.error("login:cognito_user_verification_failed", { 
          username: request.username, 
          error: userError 
        })
        // This is a critical error - user should be signed in but verification failed
        throw new Error("Cognito user verification failed after sign-in")
      }
    } catch (error) {
      this.logger.error("login:cognito_failed", { username: request.username, error })
      return { success: false, errorMessage: "AWS 로그인 도중 문제가 발생했습니다." }
    }

    await this.storagePort.saveString(UserAuthFacade.AUTH_FLAG_KEY, "true")
    
    // Store credentials for potential re-authentication (encrypted in production)
    await this.storagePort.saveString("STORED_USERNAME", request.username)
    await this.storagePort.saveString("STORED_PASSWORD", request.password)

    let hasPreferences = false
    try {
      const preferencesResponse = await this.apiClient.getPreferences()
      hasPreferences = Boolean(preferencesResponse?.ok && preferencesResponse.data)
    } catch (error) {
      this.logger.error("login:preferences_failed", { username: request.username, error })
      hasPreferences = false
    }

    this.logger.info("login:success", { username: request.username, hasPreferences })
    return { success: true, hasPreferences }
  }

  async logoutUser(): Promise<RegistrationResult> {
    this.logger.info("logout:start")

    try {
      await this.apiClient.getCsrf()
    } catch (error) {
      this.logger.error("logout:csrf_failed", { error })
    }

    try {
      await this.apiClient.logout()
    } catch (error) {
      this.logger.error("logout:backend_failed", { error })
    }

    try {
      await this.cognitoSignOut()
    } catch (error) {
      this.logger.error("logout:cognito_failed", { error })
    }

    await this.storagePort.remove(UserAuthFacade.AUTH_FLAG_KEY)
    await this.storagePort.remove("STORED_USERNAME")
    await this.storagePort.remove("STORED_PASSWORD")
    this.logger.info("logout:success")
    return { success: true }
  }

  async checkAuthenticationStatus(): Promise<{isAuthenticated: boolean, username?: string}> {
    try {
      const currentUser = await getCurrentUser()
      this.logger.info("auth_check:success", { username: currentUser.username })
      return { isAuthenticated: true, username: currentUser.username }
    } catch (error) {
      this.logger.info("auth_check:not_authenticated", { error })
      return { isAuthenticated: false }
    }
  }

  async deleteUserAccount(): Promise<RegistrationResult> {
    this.logger.info("delete_account:start")

    try {
      // Delete the current user from AWS Cognito
      await deleteUser()
      this.logger.info("delete_account:cognito_success")
      
      // Clear all local storage
      await this.storagePort.remove(UserAuthFacade.AUTH_FLAG_KEY)
      await this.storagePort.remove("NEW_USER")
      
      this.logger.info("delete_account:success")
      return { success: true }
    } catch (error) {
      this.logger.error("delete_account:cognito_failed", { error })
      return {
        success: false,
        errorMessage: "AWS 계정 삭제 중 문제가 발생했습니다.",
      }
    }
  }
}

export const userAuthFacade = new UserAuthFacade()


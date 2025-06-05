"use client"

import { createContext, useContext, useEffect, useState } from "react"

interface AuthContextType {
    isAuthenticated: boolean
    username: string | null
    token: string | null
    role: string | null
    isLoading: boolean
    login: (token: string, username: string) => void
    logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

// Helper function to decode JWT token
function parseJwt(token: string) {
    try {
        const base64Url = token.split('.')[1]
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
        const jsonPayload = decodeURIComponent(
            atob(base64)
                .split('')
                .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join('')
        )
        return JSON.parse(jsonPayload)
    } catch (e) {
        return null
    }
}

// Helper function to check if JWT token is expired
function isTokenExpired(token: string): boolean {
    try {
        const decoded = parseJwt(token)
        if (!decoded || !decoded.exp) {
            return true
        }
        // JWT exp is in seconds, Date.now() is in milliseconds
        return decoded.exp * 1000 < Date.now()
    } catch (e) {
        return true
    }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [isAuthenticated, setIsAuthenticated] = useState(false)
    const [username, setUsername] = useState<string | null>(null)
    const [token, setToken] = useState<string | null>(null)
    const [role, setRole] = useState<string | null>(null)
    const [isLoading, setIsLoading] = useState(true)

    // Check for existing token on mount
    useEffect(() => {
        const savedToken = typeof window !== 'undefined' ? localStorage.getItem('authToken') : null
        const savedUsername = typeof window !== 'undefined' ? localStorage.getItem('username') : null
        
        if (savedToken && savedUsername) {
            // Check if token is expired
            if (isTokenExpired(savedToken)) {
                // Clear expired token
                if (typeof window !== 'undefined') {
                    localStorage.removeItem('authToken')
                    localStorage.removeItem('username')
                }
                setIsLoading(false)
                return
            }
            
            setToken(savedToken)
            setUsername(savedUsername)
            setIsAuthenticated(true)
            
            // Extract role from JWT token
            const decoded = parseJwt(savedToken)
            if (decoded && decoded.role) {
                setRole(decoded.role)
            }
        }
        
        setIsLoading(false)
    }, [])

    const login = (newToken: string, newUsername: string) => {
        if (typeof window !== 'undefined') {
            localStorage.setItem('authToken', newToken)
            localStorage.setItem('username', newUsername)
        }
        setToken(newToken)
        setUsername(newUsername)
        setIsAuthenticated(true)
        
        // Extract role from JWT token
        const decoded = parseJwt(newToken)
        if (decoded && decoded.role) {
            setRole(decoded.role)
        }
    }

    const logout = () => {
        if (typeof window !== 'undefined') {
            localStorage.removeItem('authToken')
            localStorage.removeItem('username')
        }
        setToken(null)
        setUsername(null)
        setRole(null)
        setIsAuthenticated(false)
    }

    return (
        <AuthContext.Provider value={{
            isAuthenticated,
            username,
            token,
            role,
            isLoading,
            login,
            logout
        }}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    const context = useContext(AuthContext)
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider')
    }
    return context
}
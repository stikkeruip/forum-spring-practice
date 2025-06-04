"use client"

import { useState, useRef } from "react"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useToast } from "@/hooks/use-toast"
import { useAuth } from "@/components/auth-provider"
import { Loader2, ArrowLeft, Eye, EyeOff, User, LogOut } from "lucide-react"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Notifications } from "@/components/notifications"

type AuthStep = 'username' | 'login' | 'register'

interface AuthState {
    step: AuthStep
    username: string
    password: string
    isLoading: boolean
    userExists: boolean | null
}

export default function ProgressiveAuth() {
    const { isAuthenticated, username, logout, login, token } = useAuth()
    
    const [state, setState] = useState<AuthState>({
        step: 'username',
        username: '',
        password: '',
        isLoading: false,
        userExists: null
    })

    const [showPassword, setShowPassword] = useState(false)
    const [errors, setErrors] = useState<Record<string, string>>({})
    const [authError, setAuthError] = useState<string | null>(null)
    const { toast } = useToast()

    // Refs for form inputs
    const usernameRef = useRef<HTMLInputElement>(null)
    const passwordRef = useRef<HTMLInputElement>(null)

    // If user is authenticated, show profile button
    if (isAuthenticated) {
        return (
            <div className="flex items-center gap-3">
                <Notifications token={token || undefined} />
                <div className="flex items-center gap-2">
                    <Avatar className="h-8 w-8 ring-2 ring-violet-500/50">
                        <AvatarFallback className="bg-gradient-to-br from-violet-500 to-indigo-600 text-white text-sm">
                            {username ? username.substring(0, 1).toUpperCase() : 'U'}
                        </AvatarFallback>
                    </Avatar>
                    <span className="text-sm text-muted-foreground">
                        Welcome, <Link href={`/profile/${username}`} className="font-medium text-primary hover:underline">{username}</Link>
                    </span>
                </div>
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={logout}
                    className="flex items-center gap-2"
                >
                    <LogOut className="h-4 w-4" />
                    Sign Out
                </Button>
            </div>
        )
    }

    // Validate username format
    const isValidUsername = (username: string) => {
        return username.length >= 3 && /^[a-zA-Z0-9_]+$/.test(username)
    }

    // Check if user exists via API call
    const checkUserExists = async (username: string): Promise<boolean> => {
        try {
            const response = await fetch(`/api/users/exists?username=${encodeURIComponent(username)}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                throw new Error('Failed to check user existence');
            }

            const data = await response.json();
            return data.exists;
        } catch (error) {
            console.error('Error checking if user exists:', error);
            throw new Error('Unable to verify username. Please check your connection and try again.');
        }
    }

    // Handle username submission
    const handleUsernameContinue = async () => {
        if (!isValidUsername(state.username)) {
            setErrors({ username: 'Username must be at least 3 characters and contain only letters, numbers, and underscores' })
            return
        }

        setErrors({})
        setState(prev => ({ ...prev, isLoading: true }))

        try {
            const userExists = await checkUserExists(state.username)

            setState(prev => ({
                ...prev,
                isLoading: false,
                userExists,
                step: userExists ? 'login' : 'register'
            }))

            // Focus password field
            setTimeout(() => passwordRef.current?.focus(), 100)

        } catch (error) {
            console.error('Error in handleUsernameContinue:', error);
            setState(prev => ({ ...prev, isLoading: false }))
            toast({
                title: "Connection Error",
                description: error.message || "Unable to verify username. Please try again.",
                variant: "destructive"
            })
        }
    }

    // Handle authentication (login/register)
    const handleAuth = async () => {
        const newErrors: Record<string, string> = {}

        if (!state.password.trim()) {
            newErrors.password = 'Password is required'
        }
        if (state.password.length < 6) {
            newErrors.password = 'Password must be at least 6 characters'
        }

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors)
            return
        }

        setErrors({})
        setAuthError(null)
        setState(prev => ({ ...prev, isLoading: true }))

        const controller = new AbortController();
        const timeoutId = setTimeout(() => {
            controller.abort();
        }, 5000); // 5 second timeout

        try {
            // Determine endpoint based on step (login or register)
            const endpoint = state.step === 'login' ? '/api/login' : '/api/register';

            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    name: state.username,
                    password: state.password
                }),
                signal: controller.signal
            });

            // Clear the timeout
            clearTimeout(timeoutId);

            if (!response.ok) {
                let errorMessage = 'Authentication failed';
                try {
                    const errorText = await response.text();
                    if (errorText) {
                        errorMessage = errorText;
                    }
                } catch (e) {
                    console.error('Error parsing authentication error response:', e);
                }
                
                // Handle specific error cases
                if (response.status === 401 && state.step === 'login') {
                    errorMessage = 'Invalid username or password';
                }
                
                // Set error state and exit early instead of throwing
                setState(prev => ({ ...prev, isLoading: false }));
                setAuthError(errorMessage);
                toast({
                    title: "Authentication Error",
                    description: errorMessage,
                    variant: "destructive"
                });
                return;
            }

            // Handle different response types based on endpoint
            if (state.step === 'login') {
                // Login returns plain text token
                const token = await response.text();
                
                // Use auth context to handle login
                login(token, state.username);
                
                toast({
                    title: "Welcome back!",
                    description: "You've been successfully signed in."
                })
                
                // Reset form state
                setState({
                    step: 'username',
                    username: '',
                    password: '',
                    isLoading: false,
                    userExists: null
                })
            } else {
                // Register returns JSON with user info
                const data = await response.json();
                
                // After successful registration, automatically log the user in
                const loginResponse = await fetch('/api/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        name: state.username,
                        password: state.password
                    })
                });
                
                if (loginResponse.ok) {
                    const token = await loginResponse.text();
                    
                    // Use auth context to handle login
                    login(token, state.username);
                    
                    toast({
                        title: "Account created!",
                        description: "Your account has been created and you're now signed in."
                    })
                    
                    // Reset form state
                    setState({
                        step: 'username',
                        username: '',
                        password: '',
                        isLoading: false,
                        userExists: null
                    })
                } else {
                    toast({
                        title: "Account created!",
                        description: "Your account has been created. Please sign in.",
                        variant: "default"
                    })
                    
                    // Switch to login mode
                    setState(prev => ({
                        ...prev,
                        step: 'login',
                        isLoading: false,
                        userExists: true
                    }))
                }
            }

        } catch (error) {
            // Clear the timeout to prevent memory leaks
            clearTimeout(timeoutId);

            console.error('Authentication error:', error);
            setState(prev => ({ ...prev, isLoading: false }));
            
            // Set error state for UI display
            setAuthError(error.message || "Something went wrong. Please try again.");
            
            // Also show toast for immediate feedback
            toast({
                title: "Authentication Error",
                description: error.message || "Something went wrong. Please try again.",
                variant: "destructive"
            });
        }
    }

    // Reset to username step
    const resetToUsername = async () => {
        setState(prev => ({
            ...prev,
            step: 'username',
            password: '',
            userExists: null
        }))
        setErrors({})
        setAuthError(null)

        // Focus username input
        setTimeout(() => usernameRef.current?.focus(), 100)
    }

    // Handle enter key
    const handleKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            if (state.step === 'username') {
                handleUsernameContinue()
            } else {
                handleAuth()
            }
        }
    }

    return (
        <>
            <div className="flex items-center gap-3">
                {/* Username Field */}
                <div className="relative">
                    <Input
                        ref={usernameRef}
                        type="text"
                        placeholder="Enter your username"
                        value={state.username}
                        onChange={(e) => setState(prev => ({ ...prev, username: e.target.value }))}
                        onKeyPress={handleKeyPress}
                        disabled={state.step !== 'username'}
                        className={`
                w-64 transition-all duration-300
                ${state.step !== 'username' ? 'bg-muted/50 text-muted-foreground' : ''}
                ${errors.username ? 'border-destructive' : ''}
              `}
                    />
                    {errors.username && (
                        <p className="absolute -bottom-5 left-0 text-xs text-destructive">
                            {errors.username}
                        </p>
                    )}
                </div>

                {/* Password Field (Login & Register) */}
                {(state.step === 'login' || state.step === 'register') && (
                    <div className="relative">
                        <Input
                            ref={passwordRef}
                            type={showPassword ? "text" : "password"}
                            placeholder="Enter your password"
                            value={state.password}
                            onChange={(e) => {
                                setState(prev => ({ ...prev, password: e.target.value }))
                                // Clear auth error when user starts typing
                                if (authError) setAuthError(null)
                            }}
                            onKeyPress={handleKeyPress}
                            className={`w-48 pr-10 ${errors.password || authError ? 'border-destructive' : ''}`}
                        />
                        <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="absolute right-1 top-1/2 h-6 w-6 -translate-y-1/2"
                            onClick={() => setShowPassword(!showPassword)}
                        >
                            {showPassword ? (
                                <EyeOff className="h-3 w-3" />
                            ) : (
                                <Eye className="h-3 w-3" />
                            )}
                        </Button>
                        {(errors.password || authError) && (
                            <p className="absolute -bottom-5 left-0 text-xs text-destructive">
                                {errors.password || authError}
                            </p>
                        )}
                    </div>
                )}

                {/* Back Button (Login & Register) */}
                {(state.step === 'login' || state.step === 'register') && (
                    <Button
                        variant="ghost"
                        size="icon"
                        onClick={resetToUsername}
                        disabled={state.isLoading}
                        className="h-9 w-9"
                    >
                        <ArrowLeft className="h-4 w-4" />
                    </Button>
                )}

                {/* Main Action Button */}
                <Button
                    onClick={state.step === 'username' ? handleUsernameContinue : handleAuth}
                    disabled={state.isLoading}
                    className="bg-gradient-to-r from-violet-500 to-indigo-600 hover:from-violet-600 hover:to-indigo-700 text-white"
                >
                    {state.isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                    {state.step === 'username' && 'Continue'}
                    {state.step === 'login' && 'Sign In'}
                    {state.step === 'register' && 'Create Account'}
                </Button>

                {/* Status Text */}
                {(state.step === 'login' || state.step === 'register') && (
                    <div className="text-xs text-muted-foreground">
                        {state.step === 'login' ? (
                            <span>Welcome back, <span className="font-medium">{state.username}</span>!</span>
                        ) : (
                            <span>Creating account for <span className="font-medium">{state.username}</span></span>
                        )}
                    </div>
                )}
            </div>
        </>
    )
}

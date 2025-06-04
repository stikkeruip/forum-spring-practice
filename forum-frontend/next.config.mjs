/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true, // Re-enabled with singleton pattern
  eslint: {
    ignoreDuringBuilds: true,
  },
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
  },
  webpack: (config, { isServer }) => {
    if (!isServer) {
      config.resolve.fallback = {
        ...config.resolve.fallback,
        fs: false,
        path: false,
        crypto: false,
        stream: false,
        util: false,
      };
      
      config.ignoreWarnings = [
        /Module not found: Can't resolve 'supports-color'/,
        /Critical dependency: the request of a dependency is an expression/,
      ];
    }
    return config;
  },
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/:path*',
      },
    ];
  },
}

export default nextConfig

module.exports = {
  publicPath: process.env.NODE_ENV === 'production' ? '/frontend' : '/',
  outputDir: '../public/frontend',
  devServer: {
    proxy: 'http://localhost:9000'
  }
}

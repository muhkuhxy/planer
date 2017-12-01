function handleUnauthorized (err) {
  if (err.message === '401') {
    this.$router.push('/login')
  } else {
    console.log('unhandled error', err)
  }
}

export {
  handleUnauthorized
}

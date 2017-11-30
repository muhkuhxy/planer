function handleUnauthorized (err) {
  if (err.message === 'Unauthorized') {
    this.$router.push('/login')
  } else {
    console.log('unhandled error', err)
  }
}

export {
  handleUnauthorized
}

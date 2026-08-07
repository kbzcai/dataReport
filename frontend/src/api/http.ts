import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 15000 })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('reporting_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body && Number(body.code) !== 0 && Number(body.code) !== 200) {
      return Promise.reject(new Error(String(body.message || '请求失败')))
    }
    return body && typeof body === 'object' && 'data' in body ? body.data : body
  },
  (error) => Promise.reject(new Error(error.response?.data?.message || error.message || '网络请求失败')),
)

export default http

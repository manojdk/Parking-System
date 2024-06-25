import axios from 'axios';

const API_URL = 'http://localhost:8080/api/users';

const registerUser = async (userData) => {
  try {
    const response = await axios.post(`${API_URL}/register`, userData, {
      headers: {
        'Content-Type': 'application/json',
      },
      withCredentials: true, // Include this if your backend requires credentials (e.g., cookies)
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};

export default {
  registerUser,
};

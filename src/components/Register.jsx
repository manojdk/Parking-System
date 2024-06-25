import React, { useState } from 'react';
import UserService from '../services/UserService';

const Register = () => {
  const [userName, setUserName] = useState('');
  const [userEmail, setUserEmail] = useState('');
  const [password, setPassword] = useState('');
  const [licencePlate, setLicencePlate] = useState('');

  const handleRegister = async (e) => {
    e.preventDefault();
    try {
      const userData = {
        userName,
        userEmail,
        password,
        licencePlate, // Ensure this matches the backend's expected field name
      };
      console.log("Registering user with data:", userData); // Log the data
      const response = await UserService.registerUser(userData);
      console.log(response);
      // Handle successful registration
    } catch (error) {
      console.error("Error registering user:", error.response ? error.response.data : error.message);
      // Handle error
    }
  };

  return (
    <div>
      <h2>Register</h2>
      <form onSubmit={handleRegister}>
        <div>
          <label>Username:</label>
          <input type="text" value={userName} onChange={(e) => setUserName(e.target.value)} />
        </div>
        <div>
          <label>Email:</label>
          <input type="email" value={userEmail} onChange={(e) => setUserEmail(e.target.value)} />
        </div>
        <div>
          <label>Password:</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        <div>
          <label>License Plate:</label>
          <input type="text" value={licencePlate} onChange={(e) => setLicencePlate(e.target.value)} />
        </div>
        <button type="submit">Register</button>
      </form>
     
    </div>
  );
};

export default Register;

// src/App.jsx
import React from 'react';
import { Link } from 'react-router-dom';

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <h1>Welcome to User Registration</h1>
        <Link to="/register">Go to Register</Link>
      </header>
    </div>
  );
}

export default App;

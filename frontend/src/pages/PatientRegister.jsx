import React from 'react';
import PatientForm from '../components/forms/PatientForm';
import '../styles/Form.css';

const PatientRegister = () => {
    return (
        <>
            <div className="form-container">
                <PatientForm />
            </div>
        </>
    );
};

export default PatientRegister;
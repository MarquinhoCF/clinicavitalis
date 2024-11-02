package com.clinicavitalis.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinicavitalis.backend.patient.Patient;
import com.clinicavitalis.backend.patient.PatientRepository;
import com.clinicavitalis.backend.patient.PatientRequestDTO;
import com.clinicavitalis.backend.patient.PatientResponseDTO;
import com.clinicavitalis.backend.utils.EncryptionUtils;
import com.clinicavitalis.backend.utils.ValidationUtils;

@RestController
@RequestMapping("/api/patient")
public class PatientController {
    
    @Autowired
    private PatientRepository repository;

    private static final SecretKey secretKey;

    static {
        try {
            secretKey = EncryptionUtils.getSecretKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate secret key", e);
        }
    }

    //@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @PostMapping
    public ResponseEntity<String> savePatient(@RequestBody PatientRequestDTO data){

        Optional<Patient> existingPatient = repository.findByCpf(data.cpf().replaceAll("\\D", ""));
        if (existingPatient.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Erro: O CPF já está cadastrado para outro paciente.");
        }

        if (data.name() == null || data.name().isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Erro: O campo 'Nome' é obrigatório.");
        }
        
        if (data.birthdate() != null &&  !ValidationUtils.isValidBirthdate(data.birthdate())) {
            System.out.println("Aloooouuu 3.1");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Erro: A data de nascimento informada é inválida.");
        }
        
        if (data.cpf() == null || data.cpf().isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Erro: O campo 'CPF' é obrigatório.");
        } else if (!ValidationUtils.isValidCPF(data.cpf())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Erro: O CPF informado é inválido.");
        }
        
        if (data.uf() == null || data.uf().isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Erro: O campo 'UF' é obrigatório.");
        }

        String encryptedCpf;
        try {
            encryptedCpf = EncryptionUtils.encrypt(data.cpf().replaceAll("\\D", ""), secretKey);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao criptografar o CPF.");
        }
 
        // Salvar paciente com CPF criptografado
        Patient patientData = new Patient(data);
        patientData.setCpf(encryptedCpf);
        repository.save(patientData);

        // Resposta de sucesso
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Paciente cadastrado com sucesso!");
    }

    //@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @GetMapping
    public List<PatientResponseDTO> getAll(){

        List<Patient> patients = repository.findAll();

        List<PatientResponseDTO> patientList = patients.stream().map(patient -> {
            try {
                String decryptedCpf = EncryptionUtils.decrypt(patient.getCpf(), secretKey);
                return new PatientResponseDTO(patient.getId(), patient.getName(), decryptedCpf, patient.getBirthdate(), patient.getWeight(), patient.getHeight(), patient.getUf());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).filter(dto -> dto != null).toList(); 

        return patientList;
    }

    //@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @GetMapping("/countByState")
    public ResponseEntity<Map<String, Integer>> countPatientsByState() {

        Map<String, Integer> stateCounts = new HashMap<>();

        // Inicializa o mapa com todas as siglas de estados com contagem zero
        Patient.initStateCounts(stateCounts);

        List<Patient> patients = repository.findAll();

        // Conta os pacientes por estado
        for (Patient patient : patients) {
            String uf = patient.getUf();
            if (uf != null) {
                stateCounts.put(uf, stateCounts.get(uf) + 1);
            }
        }

        return ResponseEntity.ok(stateCounts);
    }

}

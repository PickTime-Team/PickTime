package com.b101.pickTime.db.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chords")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer chordId;

}
